package by.shaaldy.bot.config.kafka;

import java.util.HashMap;
import java.util.Map;

import by.shaaldy.bot.config.AppProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import by.shaaldy.bot.dto.bot.LinkUpdate;

@Configuration
@ConditionalOnProperty(name = "app.message-transport", havingValue = "KAFKA")
public class KafkaConsumerConfig {

  // --- Producer для DLQ (recoverer публикует туда битые сообщения) ---
  @Bean
  public KafkaTemplate<Object, Object> dlqKafkaTemplate(KafkaProperties props) {
    Map<String, Object> cfg = props.buildProducerProperties(null);
    cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

    Map<Class<?>, Serializer<?>> delegates = new HashMap<>();
    delegates.put(byte[].class, new ByteArraySerializer());
    delegates.put(LinkUpdate.class, new JsonSerializer<>());
    DelegatingByTypeSerializer valueSerializer = new DelegatingByTypeSerializer(delegates);

    DefaultKafkaProducerFactory<Object, Object> pf =
        new DefaultKafkaProducerFactory<>(cfg, null, valueSerializer);
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
      KafkaTemplate<Object, Object> dlqKafkaTemplate, AppProperties properties) {

    String dlqTopic = properties.kafka().topics().updatesDlq();

    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            dlqKafkaTemplate,
            (record, exception) -> new TopicPartition(dlqTopic, record.partition()));

    return new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0L));
  }
}
