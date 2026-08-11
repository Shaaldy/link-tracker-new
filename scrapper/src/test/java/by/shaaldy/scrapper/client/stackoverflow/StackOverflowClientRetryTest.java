package by.shaaldy.scrapper.client.stackoverflow;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import by.shaaldy.scrapper.config.AppProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

class StackOverflowClientRetryTest {

  private static WireMockServer wireMock;
  private static StackOverflowClient client;

  @BeforeAll
  static void startWireMock() {
    wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    wireMock.start();

    RestClient restClient = RestClient.builder().baseUrl(wireMock.baseUrl()).build();
    StackOverflowApi api =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(StackOverflowApi.class);

    RetryConfig retryConfig =
        RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            .retryOnException(
                throwable ->
                    throwable instanceof RestClientResponseException e
                        && List.of(429, 500, 502, 503, 504).contains(e.getStatusCode().value()))
            .build();
    RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);

    AppProperties.StackOverflow stackOverflowProps =
        new AppProperties.StackOverflow(wireMock.baseUrl(), null);
    AppProperties properties =
        new AppProperties(null, stackOverflowProps, null, null, null, null, null, null, null, null);

    client =
        new StackOverflowClient(
            api, properties, retryRegistry, CircuitBreakerRegistry.ofDefaults());
  }

  @AfterAll
  static void stopWireMock() {
    wireMock.stop();
  }

  @AfterEach
  void resetScenarios() {
    wireMock.resetAll();
  }

  @Test
  void fetchLastActivity_retriesOnServerErrorThenSucceeds() {
    wireMock.stubFor(
        get(urlPathEqualTo("/questions/12345"))
            .inScenario("so-retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("SECOND_ATTEMPT"));

    wireMock.stubFor(
        get(urlPathEqualTo("/questions/12345"))
            .inScenario("so-retry")
            .whenScenarioStateIs("SECOND_ATTEMPT")
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

    Instant result =
        client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345"));

    assertThat(result).isEqualTo(Instant.ofEpochSecond(1735689600));
    wireMock.verify(2, getRequestedFor(urlPathEqualTo("/questions/12345")));
  }

  @Test
  void fetchLastActivity_doesNotRetryOnNonRetryableStatus() {
    wireMock.stubFor(
        get(urlPathEqualTo("/questions/12345")).willReturn(aResponse().withStatus(400)));

    org.junit.jupiter.api.Assertions.assertThrows(
        RestClientResponseException.class,
        () -> client.fetchLastActivity(URI.create("https://stackoverflow.com/questions/12345")));

    wireMock.verify(1, getRequestedFor(urlPathEqualTo("/questions/12345")));
  }
}
