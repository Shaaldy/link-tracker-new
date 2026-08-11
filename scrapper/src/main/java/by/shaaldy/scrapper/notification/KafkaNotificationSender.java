package by.shaaldy.scrapper.notification;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import by.shaaldy.scrapper.config.AppProperties;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;
import by.shaaldy.scrapper.exception.NotificationSendException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaNotificationSender implements NotificationSender {

  private final KafkaTemplate<String, LinkUpdate> kafkaTemplate;
  private final AppProperties properties;

  @Override
  public void send(LinkUpdate update) {
    String topic = properties.kafka().topics().updates();
    String key = String.valueOf(update.getId());
    try {
      kafkaTemplate
          .send(topic, key, update)
          .get(properties.notification().kafkaSendTimeout().toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new NotificationSendException("Прервана отправка в Kafka", e);
    } catch (ExecutionException | TimeoutException e) {
      throw new NotificationSendException("Не удалось отправить уведомление в Kafka", e);
    }
  }
}
