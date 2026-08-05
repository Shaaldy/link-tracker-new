package by.shaaldy.bot.service.digest;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import by.shaaldy.bot.client.ScrapperClient;
import by.shaaldy.bot.config.AppProperties;
import by.shaaldy.bot.telegram.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Часовой флаш накопленных DIGEST-уведомлений (бонус: батчинг). Раз в час запрашивает у scrapper
 * чаты, чьё время дайджеста наступило, забирает накопленное из {@link DigestBuffer}, отправляет
 * одним сообщением, очищает буфер.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DigestScheduler {

  private final ScrapperClient scrapperClient;
  private final DigestBuffer digestBuffer;
  private final MessageSender messageSender;
  private final AppProperties properties;

  @Scheduled(cron = "0 0 * * * *")
  public void flush() {
    ZoneId zone = ZoneId.of(properties.digest().zone());
    int hour = ZonedDateTime.now(zone).getHour();
    List<Long> recipients = scrapperClient.findDigestRecipients(hour);

    if (recipients.isEmpty()) {
      log.info("Дайджест-флаш ({}:00): получателей нет", hour);
      return;
    }

    int sent = 0;
    for (Long chatId : recipients) {
      if (flushOne(chatId)) {
        sent++;
      }
    }
    log.info("Дайджест-флаш ({}:00): отправлено {} из {} чатов", hour, sent, recipients.size());
  }

  private boolean flushOne(long chatId) {
    List<String> messages = digestBuffer.get(chatId);
    if (messages.isEmpty()) {
      return false;
    }
    String digest = String.join("\n\n---\n\n", messages);
    messageSender.send(chatId, "Дайджест уведомлений:\n\n" + digest);
    digestBuffer.clear(chatId);
    return true;
  }
}
