package by.shaaldy.bot.notification;

import org.springframework.stereotype.Component;

import by.shaaldy.bot.dto.bot.LinkUpdate;
import by.shaaldy.bot.telegram.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateProcessor {

  private final MessageSender messageSender;

  public void process(LinkUpdate update) {
    update
        .getTgChatIds()
        .forEach(
            id ->
                messageSender.send(
                    id,
                    "Обновление по ссылке " + update.getUrl() + "\n" + update.getDescription()));
    log.info(
        "Обработано обновление ссылки {} для {} чатов",
        update.getUrl(),
        update.getTgChatIds().size());
  }
}
