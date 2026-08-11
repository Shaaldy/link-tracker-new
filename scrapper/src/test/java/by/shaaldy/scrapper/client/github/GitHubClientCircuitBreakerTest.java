package by.shaaldy.scrapper.client.github;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

class GitHubClientCircuitBreakerTest {

  private static WireMockServer wireMock;
  private static GitHubApi api;
  private static RetryRegistry retryRegistry;

  @BeforeAll
  static void startWireMock() {
    wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    wireMock.start();

    ClientHttpRequestFactorySettings settings =
        ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(Duration.ofSeconds(1))
            .withReadTimeout(Duration.ofMillis(300));
    RestClient restClient =
        RestClient.builder()
            .baseUrl(wireMock.baseUrl())
            .requestFactory(ClientHttpRequestFactoryBuilder.simple().build(settings))
            .build();
    api =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(GitHubApi.class);

    retryRegistry = RetryRegistry.of(RetryConfig.custom().maxAttempts(1).build());
  }

  @AfterAll
  static void stopWireMock() {
    wireMock.stop();
  }

  private CircuitBreakerRegistry newCircuitBreakerRegistry(Duration waitDurationInOpenState) {
    CircuitBreakerConfig cbConfig =
        CircuitBreakerConfig.custom()
            .slidingWindowSize(1)
            .minimumNumberOfCalls(1)
            .failureRateThreshold(100)
            .permittedNumberOfCallsInHalfOpenState(1)
            .waitDurationInOpenState(waitDurationInOpenState)
            .build();
    return CircuitBreakerRegistry.of(cbConfig);
  }

  @AfterEach
  void resetStubs() {
    wireMock.resetAll();
  }

  @Test
  void firstCall_timesOut_opensCircuitBreaker() {
    wireMock.stubFor(
        get(urlPathEqualTo("/repos/owner/repo"))
            .willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    GitHubClient client =
        new GitHubClient(api, retryRegistry, newCircuitBreakerRegistry(Duration.ofSeconds(1)));

    assertThrows(
        ResourceAccessException.class,
        () -> client.fetchLastActivity(URI.create("https://github.com/owner/repo")));
  }

  @Test
  void secondCall_afterTimeout_failsImmediatelyWithoutWaitingForTimeoutAgain() {
    wireMock.stubFor(
        get(urlPathEqualTo("/repos/owner/repo"))
            .willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    GitHubClient client =
        new GitHubClient(api, retryRegistry, newCircuitBreakerRegistry(Duration.ofSeconds(1)));

    assertThrows(
        ResourceAccessException.class,
        () -> client.fetchLastActivity(URI.create("https://github.com/owner/repo")));

    long start = System.nanoTime();
    assertThrows(
        CallNotPermittedException.class,
        () -> client.fetchLastActivity(URI.create("https://github.com/owner/repo")));
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;

    assertThat(elapsedMs).isLessThan(100);
  }

  @Test
  void openCircuit_neverReachesWireMock_onSecondCall() {
    wireMock.stubFor(
        get(urlPathEqualTo("/repos/owner/repo"))
            .willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    GitHubClient client =
        new GitHubClient(api, retryRegistry, newCircuitBreakerRegistry(Duration.ofSeconds(1)));

    assertThrows(
        ResourceAccessException.class,
        () -> client.fetchLastActivity(URI.create("https://github.com/owner/repo")));
    assertThrows(
        CallNotPermittedException.class,
        () -> client.fetchLastActivity(URI.create("https://github.com/owner/repo")));

    // CB заблокировал вызов на входе — реального запроса быть не должно.
    wireMock.verify(1, getRequestedFor(urlPathEqualTo("/repos/owner/repo")));
  }

  @Test
  void afterWaitDuration_halfOpen_successfulCallClosesCircuit() throws InterruptedException {
    wireMock.stubFor(
        get(urlPathEqualTo("/repos/owner/repo"))
            .willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    // Короткий waitDurationInOpenState, чтобы тест не ждал долго.
    GitHubClient client =
        new GitHubClient(api, retryRegistry, newCircuitBreakerRegistry(Duration.ofMillis(200)));

    assertThrows(
        ResourceAccessException.class,
        () -> client.fetchLastActivity(URI.create("https://github.com/owner/repo")));
    assertThrows(
        CallNotPermittedException.class,
        () -> client.fetchLastActivity(URI.create("https://github.com/owner/repo")));

    // Меняем стаб на быстрый успешный ответ — имитируем восстановление сервиса.
    wireMock.resetAll();
    wireMock.stubFor(
        get(urlPathEqualTo("/repos/owner/repo"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                                                {
                                                  "pushed_at": "2026-01-01T00:00:00Z",
                                                  "updated_at": "2026-01-01T00:00:00Z",
                                                  "full_name": "owner/repo",
                                                  "owner": {"login": "owner"},
                                                  "description": "repo desc"
                                                }
                                                """)));

    Thread.sleep(250); // ждём окончания waitDurationInOpenState (200ms) — CB переходит в HALF_OPEN

    var result = client.fetchLastActivity(URI.create("https://github.com/owner/repo"));
    assertThat(result).isNotNull();

    // CB снова CLOSED — следующий вызов не должен быть заблокирован.
    var secondResult = client.fetchLastActivity(URI.create("https://github.com/owner/repo"));
    assertThat(secondResult).isNotNull();
  }

  @Test
  void afterWaitDuration_halfOpen_failedCallReopensCircuit() throws InterruptedException {
    wireMock.stubFor(
        get(urlPathEqualTo("/repos/owner/repo"))
            .willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    GitHubClient client =
        new GitHubClient(api, retryRegistry, newCircuitBreakerRegistry(Duration.ofMillis(200)));

    assertThrows(
        ResourceAccessException.class,
        () -> client.fetchLastActivity(URI.create("https://github.com/owner/repo")));
    assertThrows(
        CallNotPermittedException.class,
        () -> client.fetchLastActivity(URI.create("https://github.com/owner/repo")));

    Thread.sleep(250); // ждём HALF_OPEN, стаб всё ещё медленный/падающий

    // Пробный вызов в HALF_OPEN снова падает по таймауту.
    assertThrows(
        ResourceAccessException.class,
        () -> client.fetchLastActivity(URI.create("https://github.com/owner/repo")));

    // CB должен вернуться в OPEN — следующий вызов снова блокируется мгновенно.
    long start = System.nanoTime();
    assertThrows(
        CallNotPermittedException.class,
        () -> client.fetchLastActivity(URI.create("https://github.com/owner/repo")));
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;

    assertThat(elapsedMs).isLessThan(100);
  }
}
