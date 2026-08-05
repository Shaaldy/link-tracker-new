package by.shaaldy.bot.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import by.shaaldy.bot.config.AppProperties;

class KafkaDlqIT extends AbstractKafkaIT {

  @MockitoBean by.shaaldy.bot.telegram.MessageSender messageSender;

  @Autowired KafkaTemplate<String, String> stringKafkaTemplate; // сырые строки
  @Autowired AppProperties properties;
  @Autowired KafkaProperties kafkaProperties;

  @Test
  void malformedJson_goesToDlq_andIsNotProcessed() {
    stringKafkaTemplate.send(properties.kafka().topics().updates(), "bad", "{ not a json ]");

    assertLandsInDlq();
    verify(messageSender, never()).send(anyLong(), any());
  }

  @Test
  void invalidUpdate_nullUrl_goesToDlq() {
    // валидный JSON, но url отсутствует → UpdateValidator бросит → DLQ
    stringKafkaTemplate.send(
        properties.kafka().topics().updates(), "bad", "{\"id\":1,\"tgChatIds\":[100]}");

    assertLandsInDlq();
    verify(messageSender, never()).send(anyLong(), any());
  }

  private void assertLandsInDlq() {
    Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-test-verifier");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    try (Consumer<String, String> consumer =
        new DefaultKafkaConsumerFactory<String, String>(props).createConsumer()) {
      consumer.subscribe(List.of(properties.kafka().topics().updatesDlq()));
      await()
          .atMost(Duration.ofSeconds(15))
          .untilAsserted(
              () -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                assertThat(records.count()).isGreaterThan(0);
              });
    }
  }
}
