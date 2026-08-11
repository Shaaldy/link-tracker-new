package by.shaaldy.scrapper.notification;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import by.shaaldy.scrapper.config.AppProperties;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class FallbackNotificationSender implements NotificationSender {

  private final HttpNotificationSender httpSender;
  private final KafkaNotificationSender kafkaSender;
  private final AppProperties properties;

  @Override
  public void send(LinkUpdate update) {
    NotificationSender primary = primary();
    NotificationSender secondary = secondary();
    try {
      primary.send(update);
    } catch (Exception e) {
      log.warn("Основной транспорт отказал, переключаемся на резервный", e);
      secondary.send(update);
    }
  }

  private NotificationSender primary() {
    return properties.messageTransport() == AppProperties.MessageTransport.KAFKA
        ? kafkaSender
        : httpSender;
  }

  private NotificationSender secondary() {
    return properties.messageTransport() == AppProperties.MessageTransport.KAFKA
        ? httpSender
        : kafkaSender;
  }
}
