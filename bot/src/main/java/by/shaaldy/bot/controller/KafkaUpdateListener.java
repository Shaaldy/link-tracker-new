package by.shaaldy.bot.controller;

import by.shaaldy.bot.service.UpdateValidator;
import by.shaaldy.bot.service.UpdateProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import by.shaaldy.bot.dto.bot.LinkUpdate;
import by.shaaldy.bot.service.UpdateProcessor;
import by.shaaldy.bot.service.UpdateValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.message-transport", havingValue = "KAFKA")
public class KafkaUpdateListener {

  private final UpdateProcessor processor;

  @KafkaListener(
      topics = "${app.kafka.topics.updates}",
      groupId = "${spring.kafka.consumer.group-id}")
  public void onUpdate(LinkUpdate update) {
    UpdateValidator.validate(update);
    processor.process(update);
  }
}
