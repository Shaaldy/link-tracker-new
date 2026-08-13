package by.shaaldy.scrapper.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    @Valid GitHub github,
    @Valid StackOverflow stackoverflow,
    @NotBlank String botBaseUrl,
    @NotNull AccessType accessType,
    @Valid @NotNull Scheduler scheduler,
    @NotNull MessageTransport messageTransport,
    @Valid @NotNull Kafka kafka,
    @Valid @NotNull HttpClient httpClient,
    @Valid @NotNull Retry retry,
    @Valid @NotNull CircuitBreaker circuitBreaker,
    @Valid @NotNull RateLimiter rateLimiter,
    @Valid @NotNull Notification notification,
    @Valid @NotNull Metrics metrics) {

  public record GitHub(@NotBlank String baseUrl, String token) {}

  public record StackOverflow(@NotBlank String baseUrl, String key) {}

  public record Scheduler(
      @Positive int batchSize, @NotNull Duration interval, @Positive int parallelism) {}

  public enum AccessType {
    SQL,
    ORM
  }

  public record Kafka(@Valid @NotNull Topics topics) {
    public record Topics(@NotBlank String updates, @NotBlank String updatesDlq) {}
  }

  public enum MessageTransport {
    KAFKA,
    HTTP
  }

  public record HttpClient(@Valid @NotNull Timeout timeout) {
    public record Timeout(@NotNull Duration connect, @NotNull Duration read) {}
  }

  public record Retry(
      @Positive int maxAttempts,
      @NotNull Duration waitDuration,
      @NotEmpty List<Integer> retryableStatusCodes) {}

  public record CircuitBreaker(
      @Positive int slidingWindow,
      @Positive int minimumNumberOfCalls,
      @Positive float failureRateThreshold,
      @Positive int permittedCallsInHalfOpenState,
      @NotNull Duration waitDurationOpenState) {}

  public record RateLimiter(
      @Positive int limitForPeriod,
      @NotNull Duration limitRefreshPeriod,
      @NotNull Duration timeoutDuration) {}

  public record Notification(@NotNull Duration kafkaSendTimeout) {}

  public record Metrics(@NotNull Duration linksRefreshInterval) {}
}
