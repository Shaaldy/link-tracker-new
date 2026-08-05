package by.shaaldy.bot.it;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import by.shaaldy.bot.config.AppProperties;
import by.shaaldy.bot.dto.bot.LinkUpdate;
import by.shaaldy.bot.telegram.MessageSender;

class KafkaUpdateConsumerIT extends AbstractKafkaIT {

  @MockitoBean MessageSender messageSender;

  @Autowired KafkaTemplate<String, LinkUpdate> kafkaTemplate;
  @Autowired AppProperties properties;

  @Test
  void validUpdate_isConsumedAndDispatchedToSubscribers() {
    LinkUpdate update =
        new LinkUpdate()
            .id(1L)
            .url(URI.create("https://github.com/a/b"))
            .description("новый релиз")
            .instantTgChatIds(List.of(100L, 200L))
            .digestTgChatIds(List.of());

    kafkaTemplate.send(properties.kafka().topics().updates(), "1", update);

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              verify(messageSender).send(eq(100L), contains("github.com/a/b"));
              verify(messageSender).send(eq(200L), contains("github.com/a/b"));
            });
  }
}
