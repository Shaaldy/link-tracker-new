package by.shaaldy.scrapper.notification;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

import by.shaaldy.scrapper.client.bot.BotClient;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

class HttpNotificationSenderCircuitBreakerTest {

  private static WireMockServer wireMock;
  private static BotClient botClient;
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
    botClient =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(BotClient.class);

    retryRegistry = RetryRegistry.of(RetryConfig.custom().maxAttempts(1).build());
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
    wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    HttpNotificationSender sender =
        new HttpNotificationSender(
            botClient, retryRegistry, newCircuitBreakerRegistry(Duration.ofSeconds(1)));
    LinkUpdate update = new LinkUpdate().id(1L);

    assertThrows(ResourceAccessException.class, () -> sender.send(update));
  }

  @Test
  void secondCall_afterTimeout_failsImmediatelyWithoutWaitingForTimeoutAgain() {
    wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    HttpNotificationSender sender =
        new HttpNotificationSender(
            botClient, retryRegistry, newCircuitBreakerRegistry(Duration.ofSeconds(1)));
    LinkUpdate update = new LinkUpdate().id(1L);

    assertThrows(ResourceAccessException.class, () -> sender.send(update));

    long start = System.nanoTime();
    assertThrows(CallNotPermittedException.class, () -> sender.send(update));
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;

    assertThat(elapsedMs).isLessThan(100);
  }

  @Test
  void openCircuit_neverReachesWireMock_onSecondCall() {
    wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    HttpNotificationSender sender =
        new HttpNotificationSender(
            botClient, retryRegistry, newCircuitBreakerRegistry(Duration.ofSeconds(1)));
    LinkUpdate update = new LinkUpdate().id(1L);

    assertThrows(ResourceAccessException.class, () -> sender.send(update));
    assertThrows(CallNotPermittedException.class, () -> sender.send(update));

    wireMock.verify(1, postRequestedFor(anyUrl()));
  }

  @Test
  void afterWaitDuration_halfOpen_successfulCallClosesCircuit() throws InterruptedException {
    wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    HttpNotificationSender sender =
        new HttpNotificationSender(
            botClient, retryRegistry, newCircuitBreakerRegistry(Duration.ofMillis(200)));
    LinkUpdate update = new LinkUpdate().id(1L);

    assertThrows(ResourceAccessException.class, () -> sender.send(update));
    assertThrows(CallNotPermittedException.class, () -> sender.send(update));

    wireMock.resetAll();
    wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));

    Thread.sleep(250);

    sender.send(update);
    sender.send(update); // CB снова CLOSED — второй вызов не блокируется
  }

  @Test
  void afterWaitDuration_halfOpen_failedCallReopensCircuit() throws InterruptedException {
    wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withFixedDelay(1000).withStatus(200)));
    HttpNotificationSender sender =
        new HttpNotificationSender(
            botClient, retryRegistry, newCircuitBreakerRegistry(Duration.ofMillis(200)));
    LinkUpdate update = new LinkUpdate().id(1L);

    assertThrows(ResourceAccessException.class, () -> sender.send(update));
    assertThrows(CallNotPermittedException.class, () -> sender.send(update));

    Thread.sleep(250);

    assertThrows(ResourceAccessException.class, () -> sender.send(update));

    long start = System.nanoTime();
    assertThrows(CallNotPermittedException.class, () -> sender.send(update));
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;

    assertThat(elapsedMs).isLessThan(100);
  }
}
