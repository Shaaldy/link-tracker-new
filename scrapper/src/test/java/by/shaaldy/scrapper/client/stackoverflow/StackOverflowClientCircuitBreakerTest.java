package by.shaaldy.scrapper.client.stackoverflow;

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

import by.shaaldy.scrapper.config.AppProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

class StackOverflowClientCircuitBreakerTest {

  private static WireMockServer wireMock;
  private static StackOverflowApi api;
  private static AppProperties properties;
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
            .createClient(StackOverflowApi.class);

    retryRegistry = RetryRegistry.of(RetryConfig.custom().maxAttempts(1).build());

    AppProperties.StackOverflow stackOverflowProps =
        new AppProperties.StackOverflow(wireMock.baseUrl(), null);
    properties =
        new AppProperties(null, stackOverflowProps, null, null, null, null, null, null, null, null);
  }

  @AfterAll
  static void stopWireMock() {
    wireMock.stop();
  }

  @AfterEach
  void resetStubs() {
    wireMock.resetAll();
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

  @Test
  void firstCall_timesOut_opensCircuitBreaker() {
    wireMock.stubFor(
        get(urlPathEqualTo("/questions/12345"))
            .willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    StackOverflowClient client =
        new StackOverflowClient(
            api, properties, retryRegistry, newCircuitBreakerRegistry(Duration.ofSeconds(1)));

    assertThrows(
        ResourceAccessException.class,
        () -> client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345")));
  }

  @Test
  void secondCall_afterTimeout_failsImmediatelyWithoutWaitingForTimeoutAgain() {
    wireMock.stubFor(
        get(urlPathEqualTo("/questions/12345"))
            .willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    StackOverflowClient client =
        new StackOverflowClient(
            api, properties, retryRegistry, newCircuitBreakerRegistry(Duration.ofSeconds(1)));

    assertThrows(
        ResourceAccessException.class,
        () -> client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345")));

    long start = System.nanoTime();
    assertThrows(
        CallNotPermittedException.class,
        () -> client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345")));
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;

    assertThat(elapsedMs).isLessThan(100);
  }

  @Test
  void openCircuit_neverReachesWireMock_onSecondCall() {
    wireMock.stubFor(
        get(urlPathEqualTo("/questions/12345"))
            .willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    StackOverflowClient client =
        new StackOverflowClient(
            api, properties, retryRegistry, newCircuitBreakerRegistry(Duration.ofSeconds(1)));

    assertThrows(
        ResourceAccessException.class,
        () -> client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345")));
    assertThrows(
        CallNotPermittedException.class,
        () -> client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345")));

    wireMock.verify(1, getRequestedFor(urlPathEqualTo("/questions/12345")));
  }

  @Test
  void afterWaitDuration_halfOpen_successfulCallClosesCircuit() throws InterruptedException {
    wireMock.stubFor(
        get(urlPathEqualTo("/questions/12345"))
            .willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    StackOverflowClient client =
        new StackOverflowClient(
            api, properties, retryRegistry, newCircuitBreakerRegistry(Duration.ofMillis(200)));

    assertThrows(
        ResourceAccessException.class,
        () -> client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345")));
    assertThrows(
        CallNotPermittedException.class,
        () -> client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345")));

    wireMock.resetAll();
    wireMock.stubFor(
        get(urlPathEqualTo("/questions/12345"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                                                {
                                                  "items": [
                                                    {
                                                      "title": "Some question",
                                                      "last_activity_date": 1735689600,
                                                      "creation_date": 1735689600,
                                                      "body": "body text",
                                                      "owner": {"display_name": "asker"}
                                                    }
                                                  ]
                                                }
                                                """)));

    Thread.sleep(250);

    var result = client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345"));
    assertThat(result).isNotNull();

    var secondResult =
        client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345"));
    assertThat(secondResult).isNotNull();
  }

  @Test
  void afterWaitDuration_halfOpen_failedCallReopensCircuit() throws InterruptedException {
    wireMock.stubFor(
        get(urlPathEqualTo("/questions/12345"))
            .willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    StackOverflowClient client =
        new StackOverflowClient(
            api, properties, retryRegistry, newCircuitBreakerRegistry(Duration.ofMillis(200)));

    assertThrows(
        ResourceAccessException.class,
        () -> client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345")));
    assertThrows(
        CallNotPermittedException.class,
        () -> client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345")));

    Thread.sleep(250);

    assertThrows(
        ResourceAccessException.class,
        () -> client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345")));

    long start = System.nanoTime();
    assertThrows(
        CallNotPermittedException.class,
        () -> client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345")));
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;

    assertThat(elapsedMs).isLessThan(100);
  }
}
