package by.shaaldy.scrapper.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        GitHub github,
        StackOverflow stackoverflow,
        @NotBlank String botBaseUrl,
        @NotNull AccessType accessType,
        @NotNull Scheduler scheduler) {

  public record GitHub(@NotBlank String baseUrl, String token) {}

  public record StackOverflow(@NotBlank String baseUrl, String key) {}

  public record Scheduler(@Positive int batchSize, @NotNull Duration interval) {}

  public enum AccessType {
    SQL,
    ORM
  }
}