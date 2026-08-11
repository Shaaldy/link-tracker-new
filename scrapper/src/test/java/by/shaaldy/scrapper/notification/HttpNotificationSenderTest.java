package by.shaaldy.scrapper.notification;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import by.shaaldy.scrapper.client.bot.BotClient;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;

@ExtendWith(MockitoExtension.class)
class HttpNotificationSenderTest {

  @Mock BotClient botClient;

  @Test
  void send_delegatesToBotClient() {
    RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
    CircuitBreakerRegistry cb = CircuitBreakerRegistry.ofDefaults();
    HttpNotificationSender sender = new HttpNotificationSender(botClient, retryRegistry, cb);
    LinkUpdate update = new LinkUpdate().id(1L);

    sender.send(update);

    verify(botClient).sendUpdate(update);
  }
}
