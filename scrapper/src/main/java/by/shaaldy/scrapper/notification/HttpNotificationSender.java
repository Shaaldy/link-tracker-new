package by.shaaldy.scrapper.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import by.shaaldy.scrapper.client.bot.BotClient;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.message-transport", havingValue = "HTTP", matchIfMissing = true)
public class HttpNotificationSender implements NotificationSender {
  private final BotClient botClient;

  @Override
  public void send(LinkUpdate update) {
    botClient.sendUpdate(update);
  }
}
