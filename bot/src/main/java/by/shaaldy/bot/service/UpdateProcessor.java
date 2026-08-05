package by.shaaldy.bot.service;

import java.util.List;

import org.springframework.stereotype.Component;

import by.shaaldy.bot.dto.bot.LinkUpdate;
import by.shaaldy.bot.service.digest.DigestBuffer;
import by.shaaldy.bot.telegram.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateProcessor {

  private final MessageSender messageSender;
  private final DigestBuffer digestBuffer;

  public void process(LinkUpdate update) {
    String text = "Обновление по ссылке " + update.getUrl() + "\n" + update.getDescription();

    List<Long> instant = update.getInstantTgChatIds();
    List<Long> digest = update.getDigestTgChatIds();

    if (instant != null) {
      instant.forEach(id -> messageSender.send(id, text));
    }
    if (digest != null) {
      digest.forEach(id -> digestBuffer.append(id, text));
    }

    log.info(
        "Обработано обновление ссылки {}: instant={}, digest={}",
        update.getUrl(),
        instant == null ? 0 : instant.size(),
        digest == null ? 0 : digest.size());
  }
}
