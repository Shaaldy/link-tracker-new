package by.shaaldy.scrapper.client.github;

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

import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

class GitHubClientRetryTest {

  private static WireMockServer wireMock;
  private static GitHubClient client;

  @BeforeAll
  static void startWireMock() {
    wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    wireMock.start();

    RestClient restClient = RestClient.builder().baseUrl(wireMock.baseUrl()).build();
    GitHubApi api =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(GitHubApi.class);

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

    client = new GitHubClient(api, retryRegistry);
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
        get(urlPathEqualTo("/repos/owner/repo"))
            .inScenario("github-retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("SECOND_ATTEMPT"));

    wireMock.stubFor(
        get(urlPathEqualTo("/repos/owner/repo"))
            .inScenario("github-retry")
            .whenScenarioStateIs("SECOND_ATTEMPT")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                                                {
                                                  "pushed_at": "2026-01-01T00:00:00Z",
                                                  "full_name": "owner/repo",
                                                  "owner": {"login": "owner"},
                                                  "description": "repo desc"
                                                }
                                                """)));

    Instant result = client.fetchLastActivity(URI.create("https://github.com/owner/repo"));

    assertThat(result).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    wireMock.verify(2, getRequestedFor(urlPathEqualTo("/repos/owner/repo")));
  }

  @Test
  void fetchLastActivity_doesNotRetryOnNonRetryableStatus() {
    wireMock.stubFor(
        get(urlPathEqualTo("/repos/owner/repo")).willReturn(aResponse().withStatus(404)));

    org.junit.jupiter.api.Assertions.assertThrows(
        RestClientResponseException.class,
        () -> client.fetchLastActivity(URI.create("https://github.com/owner/repo")));

    wireMock.verify(1, getRequestedFor(urlPathEqualTo("/repos/owner/repo")));
  }
}
