package by.shaaldy.bot.config;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import by.shaaldy.bot.dto.bot.LinkUpdate;

@Configuration
@ConditionalOnProperty(name = "app.message-transport", havingValue = "KAFKA")
public class KafkaConsumerConfig {

  // --- Producer для DLQ (recoverer публикует туда битые сообщения) ---
  @Bean
  public KafkaTemplate<byte[], byte[]> dlqKafkaTemplate(KafkaProperties props) {
    ProducerFactory<byte[], byte[]> pf =
        new DefaultKafkaProducerFactory<>(
            props.buildProducerProperties(null),
            new org.apache.kafka.common.serialization.ByteArraySerializer(),
            new org.apache.kafka.common.serialization.ByteArraySerializer());
    return new KafkaTemplate<>(pf);
  }

  // --- Consumer factory с ErrorHandlingDeserializer поверх JsonDeserializer ---
  @Bean
  public ConsumerFactory<String, LinkUpdate> consumerFactory(KafkaProperties props) {
    Map<String, Object> cfg = props.buildConsumerProperties(null);
    cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    cfg.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    cfg.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
    cfg.put(JsonDeserializer.VALUE_DEFAULT_TYPE, LinkUpdate.class.getName());
    cfg.put(JsonDeserializer.TRUSTED_PACKAGES, "by.shaaldy.bot.dto.bot");
    cfg.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
    return new DefaultKafkaConsumerFactory<>(cfg);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, LinkUpdate> kafkaListenerContainerFactory(
      ConsumerFactory<String, LinkUpdate> consumerFactory, DefaultErrorHandler errorHandler) {
    ConcurrentKafkaListenerContainerFactory<String, LinkUpdate> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler);
    return factory;
  }

  // --- Error handler → DLQ, без ретраев (битый payload детерминирован) ---
  @Bean
  public DefaultErrorHandler errorHandler(
      KafkaTemplate<byte[], byte[]> dlqKafkaTemplate,
      by.shaaldy.bot.config.AppProperties properties) {
    String dlqTopic = properties.kafka().topics().updatesDlq();
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            dlqKafkaTemplate,
            (record, exception) -> new TopicPartition(dlqTopic, record.partition()));
    return new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0L));
  }
}
