package by.shaaldy.bot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import by.shaaldy.bot.dto.bot.LinkUpdate;
import by.shaaldy.bot.telegram.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UpdateController {

  private final MessageSender messageSender;

  @PostMapping("/updates")
  public ResponseEntity<Void> receiveUpdate(@RequestBody LinkUpdate update) {
    update
        .getTgChatIds()
        .forEach(
            id ->
                messageSender.send(
                    id,
                    "Обновление по ссылке " + update.getUrl() + "\n" + update.getDescription()));
    log.info(
        "Получено обновление ссылки {} для {} чатов",
        update.getUrl(),
        update.getTgChatIds().size());
    return ResponseEntity.ok().build();
  }
}