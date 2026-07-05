package by.shaaldy.scrapper.scheduler;

import java.net.URI;
import java.util.ArrayList;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import by.shaaldy.scrapper.client.BotClient;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;
import by.shaaldy.scrapper.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateScheduler {

  private final SubscriptionRepository repository;
  private final BotClient botClient;

  @Scheduled(fixedDelayString = "${app.scheduler-interval}")
  public void checkUpdates() {
    for (URI url : repository.findAllUrls()) {
      Set<Long> subscribers = repository.findSubscribers(url);
      if (subscribers.isEmpty()) {
        continue;
      }
      LinkUpdate update =
          new LinkUpdate()
              .url(url)
              .description("Обнаружено обновление (заглушка Stage 1)")
              .tgChatIds(new ArrayList<>(subscribers));
      botClient.sendUpdate(update);
      log.info("Отправлено обновление по {} для {} чатов", url, subscribers.size());
    }
  }
}
