package by.shaaldy.scrapper.scheduler;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import by.shaaldy.scrapper.client.LinkSourceRouter;
import by.shaaldy.scrapper.client.UpdateChecker;
import by.shaaldy.scrapper.client.bot.BotClient;
import by.shaaldy.scrapper.config.AppProperties;
import by.shaaldy.scrapper.domain.Link;
import by.shaaldy.scrapper.domain.UpdateDetails;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;
import by.shaaldy.scrapper.repository.LinkPollingRepository;
import by.shaaldy.scrapper.repository.LinkPollingRepository.Cursor;
import by.shaaldy.scrapper.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UpdateScheduler {

  private final LinkPollingRepository pollingRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final LinkSourceRouter router;
  private final BotClient botClient;
  private final AppProperties properties;
  private final Semaphore semaphore; // ← без инициализатора здесь

  @Autowired
  public UpdateScheduler(
      LinkPollingRepository pollingRepository,
      SubscriptionRepository subscriptionRepository,
      LinkSourceRouter router,
      BotClient botClient,
      AppProperties properties) {
    this.pollingRepository = pollingRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.router = router;
    this.botClient = botClient;
    this.properties = properties;
    this.semaphore =
        new Semaphore(properties.scheduler().parallelism()); // ← здесь, properties уже есть
  }

  @Scheduled(fixedDelayString = "${app.scheduler.interval}")
  public void poll() {
    Instant tickStart = Instant.now();
    int batchSize = properties.scheduler().batchSize();
    Cursor cursor = Cursor.start();

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      while (true) {
        List<Link> batch = pollingRepository.findBatch(cursor, tickStart, batchSize);
        if (batch.isEmpty()) {
          break;
        }
        List<Future<?>> futures =
            batch.stream()
                .map(link -> executor.submit(() -> checkOneGuarded(link)))
                .collect(Collectors.toList());
        awaitAll(futures);

        Link last = batch.getLast();
        cursor = new Cursor(last.getLastCheckedAt(), last.getId());
        if (batch.size() < batchSize) {
          break;
        }
      }
    }
  }

  private void awaitAll(List<Future<?>> futures) {
    for (Future<?> f : futures) {
      try {
        f.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (java.util.concurrent.ExecutionException e) {
        log.warn("Ошибка в задаче обработки ссылки: {}", e.getCause().getMessage());
      }
    }
  }

  private void checkOneGuarded(Link link) {
    try {
      semaphore.acquire();
      try {
        checkOne(link);
      } finally {
        semaphore.release();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Прервана проверка {}", link.getUrl());
    } catch (RuntimeException e) {
      log.warn("Не удалось проверить {}: {}", link.getUrl(), e.getMessage());
    }
  }

  private void checkOne(Link link) {
    URI url = link.getUrl();
    UpdateChecker checker = router.route(url);

    Instant latest = checker.fetchLastActivity(url);
    if (!latest.isAfter(link.getLastCheckedAt())) {
      return; // обновлений нет
    }

    Set<Long> subscribers = subscriptionRepository.findSubscribers(url);
    if (!subscribers.isEmpty()) {
      UpdateDetails details = checker.fetchDetails(url);
      LinkUpdate update =
          new LinkUpdate()
              .id(link.getId())
              .url(url)
              .description(format(url, details))
              .tgChatIds(List.copyOf(subscribers));
      botClient.sendUpdate(update);
      log.info("Отправлено обновление по {} для {} подписчиков", url, subscribers.size());
    }

    pollingRepository.updateCheckedAt(link.getId(), latest);
  }

  /** Форматирует детализацию в человекочитаемое сообщение для бота (требование ДЗ). */
  private static String format(URI url, UpdateDetails d) {
    if (d == null) {
      return "Обновление: " + url;
    }
    StringBuilder sb = new StringBuilder("Обновление: ").append(url);
    if (d.title() != null) {
      sb.append("\n").append(d.title());
    }
    if (d.author() != null) {
      sb.append("\nАвтор: ").append(d.author());
    }
    if (d.createdAt() != null) {
      sb.append("\nСоздано: ").append(DateTimeFormatter.ISO_INSTANT.format(d.createdAt()));
    }
    if (d.preview() != null && !d.preview().isBlank()) {
      sb.append("\n").append(d.preview());
    }
    return sb.toString();
  }
}
