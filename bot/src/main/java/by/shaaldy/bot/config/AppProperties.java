package by.shaaldy.bot.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@NotBlank String telegramToken, @NotBlank String scrapperBaseUrl, @NotNull MessageTransport messageTransport,
                            @Valid @NotNull Kafka kafka,
                            @Valid @NotNull Cache cache) {
    public record Kafka(@Valid @NotNull Topics topics) {

        public record Topics(@NotBlank String updates, @NotBlank String updatesDlq) {}
    }
    public record Cache(@NotNull Duration listTtl) {}

    public enum MessageTransport {
        KAFKA,
        HTTP
    }
}
