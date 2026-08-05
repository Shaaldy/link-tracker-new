package by.shaaldy.bot.config;

import java.time.Duration;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    @NotBlank String telegramToken,
    @NotBlank String scrapperBaseUrl,
    @NotNull MessageTransport messageTransport,
    @Valid @NotNull Kafka kafka,
    @Valid @NotNull Cache cache,
    @Valid @NotNull Digest digest,
    @Valid @NotNull HttpClient httpClient,
    @Valid @NotNull Retry retry) {
  public record Kafka(@Valid @NotNull Topics topics) {

    public record Topics(@NotBlank String updates, @NotBlank String updatesDlq) {}
  }

  public record Cache(boolean enabled, @NotNull Duration listTtl) {}

  public enum MessageTransport {
    KAFKA,
    HTTP
  }

  public record Digest(@NotBlank String zone) {}

  public record HttpClient(@Valid @NotNull Timeout timeout) {
    public record Timeout(@NotNull Duration connect, @NotNull Duration read) {}
  }
  public record Retry(
          @Positive int maxAttempts,
          @NotNull Duration waitDuration,
          @NotEmpty List<Integer> retryableStatusCodes) {}
}
