package by.shaaldy.scrapper.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import by.shaaldy.scrapper.config.AppProperties;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.message-transport", havingValue = "KAFKA")
public class KafkaNotificationSender implements NotificationSender {

  private final KafkaTemplate<String, LinkUpdate> kafkaTemplate;
  private final AppProperties properties;

  @Override
  public void send(LinkUpdate update) {
    String topic = properties.kafka().topics().updates();
    String key = String.valueOf(update.getId());
    kafkaTemplate.send(topic, key, update);
  }
}
