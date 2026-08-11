package by.shaaldy.scrapper.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import by.shaaldy.scrapper.client.bot.BotClient;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.message-transport", havingValue = "HTTP", matchIfMissing = true)
public class HttpNotificationSender implements NotificationSender {
  private final BotClient botClient;
  private final RetryRegistry retryRegistry;
  private final CircuitBreakerRegistry circuitBreakerRegistry;

  @Override
  public void send(LinkUpdate update) {
    circuitBreakerRegistry
        .circuitBreaker("bot-sendUpdate")
        .executeRunnable(
            () ->
                retryRegistry
                    .retry("bot-sendUpdate")
                    .executeRunnable(() -> botClient.sendUpdate(update)));
  }
}
