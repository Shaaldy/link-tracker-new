package by.shaaldy.bot.it;

import java.util.Map;

import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import by.shaaldy.bot.dto.bot.LinkUpdate;

@TestConfiguration
public class KafkaTestConfig {

  // Типизированный — для валидного кейса (сериализует LinkUpdate в JSON, как scrapper)
  @Bean
  public KafkaTemplate<String, LinkUpdate> testKafkaTemplate(KafkaProperties props) {
    Map<String, Object> cfg = props.buildProducerProperties(null);
    ProducerFactory<String, LinkUpdate> pf =
        new DefaultKafkaProducerFactory<>(cfg, new StringSerializer(), new JsonSerializer<>());
    return new KafkaTemplate<>(pf);
  }

  // Строковый — для DLQ-кейса (шлём заведомо битый payload как есть)
  @Bean
  public KafkaTemplate<String, String> stringKafkaTemplate(KafkaProperties props) {
    Map<String, Object> cfg = props.buildProducerProperties(null);
    ProducerFactory<String, String> pf =
        new DefaultKafkaProducerFactory<>(cfg, new StringSerializer(), new StringSerializer());
    return new KafkaTemplate<>(pf);
  }
}
