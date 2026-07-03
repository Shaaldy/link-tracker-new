package by.shaaldy.scrapper.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(GitHub github, StackOverflow stackOverflow){
    public record GitHub(@NotBlank String baseUrl, String token) {}
    public record StackOverflow(@NotBlank String baseUrl, String key) {}
}
