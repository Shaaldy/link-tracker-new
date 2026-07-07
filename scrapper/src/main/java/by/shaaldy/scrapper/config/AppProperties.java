package by.shaaldy.scrapper.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    GitHub github,
    StackOverflow stackOverflow,
    @NotBlank String botBaseUrl,
    @NotNull Duration schedulerInterval) {
  public record GitHub(@NotBlank String baseUrl, String token) {}

  public record StackOverflow(@NotBlank String baseUrl, String key) {}
}
