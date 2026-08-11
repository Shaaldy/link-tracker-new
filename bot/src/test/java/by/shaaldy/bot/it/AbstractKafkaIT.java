package by.shaaldy.bot.it;

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {"app.message-transport=KAFKA", "app.telegram-token=test-token"})
@Import(KafkaTestConfig.class)
public abstract class AbstractKafkaIT {

  static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

  static {
    KAFKA.start();
    createTopics();
  }

  @DynamicPropertySource
  static void kafkaProps(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
  }

  private static void createTopics() {
    try (AdminClient admin =
        AdminClient.create(
            Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
      admin
          .createTopics(
              List.of(
                  new NewTopic("link-updates", 1, (short) 1),
                  new NewTopic("link-updates-dlq", 1, (short) 1)))
          .all()
          .get();
    } catch (Exception e) {
      throw new IllegalStateException("Не удалось создать топики для теста", e);
    }
  }
}
