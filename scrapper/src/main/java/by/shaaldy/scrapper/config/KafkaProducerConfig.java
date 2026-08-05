package by.shaaldy.scrapper.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import by.shaaldy.scrapper.dto.bot.LinkUpdate;

@Configuration
@ConditionalOnProperty(name = "app.message-transport", havingValue = "KAFKA")
public class KafkaProducerConfig {
  @Bean
  public ProducerFactory<String, LinkUpdate> producerFactory(KafkaProperties kafkaProperties) {
    return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(null));
  }

  @Bean
  public KafkaTemplate<String, LinkUpdate> kafkaTemplate(
      ProducerFactory<String, LinkUpdate> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }
}
