package by.shaaldy.scrapper.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class TransportSelectionTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(TransportConfig.class);

  @Test
  void httpByDefault_selectsHttpSender() {
    runner.run(
        ctx ->
            assertThat(ctx)
                .getBean(NotificationSender.class)
                .isInstanceOf(HttpNotificationSender.class));
  }

  @Test
  void httpExplicit_selectsHttpSender() {
    runner
        .withPropertyValues("app.message-transport=HTTP")
        .run(
            ctx ->
                assertThat(ctx)
                    .getBean(NotificationSender.class)
                    .isInstanceOf(HttpNotificationSender.class));
  }

  @Test
  void kafka_selectsKafkaSender() {
    runner
        .withPropertyValues("app.message-transport=KAFKA")
        .run(
            ctx ->
                assertThat(ctx)
                    .getBean(NotificationSender.class)
                    .isInstanceOf(KafkaNotificationSender.class));
  }

  @Configuration
  static class TransportConfig {
    @Bean
    @ConditionalOnProperty(
        name = "app.message-transport",
        havingValue = "HTTP",
        matchIfMissing = true)
    NotificationSender httpSender() {
      return new HttpNotificationSender(null); // делегата не вызываем — проверяем только тип
    }

    @Bean
    @ConditionalOnProperty(name = "app.message-transport", havingValue = "KAFKA")
    NotificationSender kafkaSender() {
      return new KafkaNotificationSender(null, null);
    }
  }
}
