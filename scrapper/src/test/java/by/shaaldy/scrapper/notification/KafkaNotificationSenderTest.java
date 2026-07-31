package by.shaaldy.scrapper.notification;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import by.shaaldy.scrapper.config.AppProperties;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;

@ExtendWith(MockitoExtension.class)
class KafkaNotificationSenderTest {

  @Mock KafkaTemplate<String, LinkUpdate> kafkaTemplate;
  @Mock AppProperties properties;

  @Test
  void send_publishesToConfiguredTopicWithLinkIdAsKey() {
    AppProperties.Kafka kafka = mock_kafka();
    when(properties.kafka()).thenReturn(kafka);

    KafkaNotificationSender sender = new KafkaNotificationSender(kafkaTemplate, properties);
    LinkUpdate update = new LinkUpdate().id(42L);

    sender.send(update);

    verify(kafkaTemplate).send("link-updates", "42", update);
  }

  private AppProperties.Kafka mock_kafka() {
    AppProperties.Kafka.Topics topics = mock(AppProperties.Kafka.Topics.class);
    when(topics.updates()).thenReturn("link-updates");
    AppProperties.Kafka kafka = mock(AppProperties.Kafka.class);
    when(kafka.topics()).thenReturn(topics);
    return kafka;
  }
}
