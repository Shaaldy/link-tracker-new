package by.shaaldy.scrapper.scheduler;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import by.shaaldy.scrapper.client.LinkSourceRouter;
import by.shaaldy.scrapper.client.bot.BotClient;
import by.shaaldy.scrapper.domain.Link;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;
import by.shaaldy.scrapper.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateScheduler {

  private final SubscriptionRepository repository;
  private final LinkSourceRouter router;
  private final BotClient botClient;

  @Scheduled(fixedDelayString = "${app.scheduler-interval}")
  public void poll() {
    repository
        .findAllLinks()
        .forEach(
            link -> {
              try {
                checkOne(link);
              } catch (RuntimeException e) {
                log.warn("Не удалось проверить {}: {}", link.getUrl(), e.getMessage());
              }
            });
  }

  private void checkOne(Link link) {
    URI url = link.getUrl();
    Instant latest = router.route(url).fetchLastActivity(url);
    Instant seen = repository.getCheckedAt(url);
    if (!latest.isAfter(seen)) {
      return;
    }

    Collection<Long> subscribers = repository.findSubscribers(url);
    if (!subscribers.isEmpty()) {
      LinkUpdate update =
          new LinkUpdate()
              .id(link.getId())
              .url(url)
              .description("Обновление: " + url)
              .tgChatIds(List.copyOf(subscribers));
      botClient.sendUpdate(update);
      log.info("Отправлено обновление по {} для {} подписчиков", url, subscribers.size());
    }

    repository.updateCheckedAt(url, latest);
  }
}
