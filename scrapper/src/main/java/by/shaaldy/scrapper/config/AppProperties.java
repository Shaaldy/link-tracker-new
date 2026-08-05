package by.shaaldy.scrapper.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
    @Valid @NotNull Kafka kafka) {

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
}
