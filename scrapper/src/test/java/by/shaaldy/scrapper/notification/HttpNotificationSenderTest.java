package by.shaaldy.scrapper.notification;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import by.shaaldy.scrapper.client.bot.BotClient;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;

@ExtendWith(MockitoExtension.class)
class HttpNotificationSenderTest {

  @Mock BotClient botClient;

  @Test
  void send_delegatesToBotClient() {
    HttpNotificationSender sender = new HttpNotificationSender(botClient);
    LinkUpdate update = new LinkUpdate().id(1L);

    sender.send(update);

    verify(botClient).sendUpdate(update);
  }
}
