package by.shaaldy.scrapper.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import by.shaaldy.scrapper.client.LinkSourceRouter;
import by.shaaldy.scrapper.client.UpdateChecker;
import by.shaaldy.scrapper.config.AppProperties;
import by.shaaldy.scrapper.domain.Link;
import by.shaaldy.scrapper.domain.UpdateDetails;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;
import by.shaaldy.scrapper.notification.NotificationSender;
import by.shaaldy.scrapper.repository.LinkPollingRepository;
import by.shaaldy.scrapper.repository.LinkPollingRepository.Cursor;
import by.shaaldy.scrapper.repository.SubscriptionRepository;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class UpdateSchedulerTest {

  @Mock LinkPollingRepository polling;
  @Mock SubscriptionRepository repository;
  @Mock LinkSourceRouter router;
  @Mock NotificationSender notificationSender;
  @Mock UpdateChecker checker;
  @Mock AppProperties properties;

  SimpleMeterRegistry registry;
  UpdateScheduler scheduler;

  private static final Instant FRESH = Instant.parse("2026-07-01T00:00:00Z");
  private static final Instant SEEN = Instant.EPOCH;

  private Link link(long id, URI url, Instant lastCheckedAt) {
    return Link.builder().id(id).url(url).lastCheckedAt(lastCheckedAt).build();
  }

  @BeforeEach
  void setUp() {
    AppProperties.Scheduler sched = mock(AppProperties.Scheduler.class);
    lenient().when(sched.batchSize()).thenReturn(100);
    lenient().when(sched.parallelism()).thenReturn(4);
    lenient().when(properties.scheduler()).thenReturn(sched);
    registry = new SimpleMeterRegistry();
    scheduler =
        new UpdateScheduler(polling, repository, router, notificationSender, properties, registry);
  }

  /** Первый findBatch отдаёт батч, второй — пусто (иначе бесконечный цикл). */
  private void singleBatch(Link... links) {
    when(polling.findBatch(any(Cursor.class), any(Instant.class), anyInt()))
        .thenReturn(List.of(links))
        .thenReturn(List.of());
  }

  @Test
  void poll_updatedLink_notifiesSubscribersSplitByMode() {
    URI url = URI.create("https://github.com/a/b");
    singleBatch(link(1L, url, SEEN));
    when(router.route(url)).thenReturn(checker);
    when(checker.fetchLastActivity(url)).thenReturn(FRESH);
    when(checker.fetchDetails(url)).thenReturn(new UpdateDetails("t", "author", FRESH, "preview"));
    when(repository.findSubscribersWithMode(url))
        .thenReturn(
            List.of(
                new SubscriptionRepository.SubscriberMode(10L, "INSTANT"),
                new SubscriptionRepository.SubscriberMode(20L, "DIGEST")));

    scheduler.poll();

    ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
    verify(notificationSender).send(captor.capture());
    assertThat(captor.getValue().getInstantTgChatIds()).containsExactly(10L);
    assertThat(captor.getValue().getDigestTgChatIds()).containsExactly(20L);
    assertThat(captor.getValue().getDescription()).contains("author", "preview");
    verify(polling).updateCheckedAt(1L, FRESH);
  }

  @Test
  void poll_nothingNewer_doesNotSend() {
    URI url = URI.create("https://github.com/a/b");
    singleBatch(link(1L, url, FRESH)); // метка уже FRESH
    when(router.route(url)).thenReturn(checker);
    when(checker.fetchLastActivity(url)).thenReturn(FRESH); // равно — не новее

    scheduler.poll();

    verify(notificationSender, never()).send(any());
    verify(polling, never()).updateCheckedAt(anyLong(), any());
    verify(checker, never()).fetchDetails(any()); // экономия: детали не тянем
  }

  @Test
  void poll_noSubscribers_movesCheckedAtWithoutSending() {
    URI url = URI.create("https://github.com/a/b");
    singleBatch(link(1L, url, SEEN));
    when(router.route(url)).thenReturn(checker);
    when(checker.fetchLastActivity(url)).thenReturn(FRESH);
    when(repository.findSubscribersWithMode(url)).thenReturn(List.of());

    scheduler.poll();

    verify(notificationSender, never()).send(any());
    verify(polling).updateCheckedAt(1L, FRESH);
  }

  @Test
  void poll_linkCheckFails_continuesWithOtherLinks() {
    URI bad = URI.create("https://github.com/broken/repo");
    URI good = URI.create("https://github.com/a/b");
    singleBatch(link(1L, bad, SEEN), link(2L, good, SEEN));
    when(router.route(bad)).thenReturn(checker);
    when(router.route(good)).thenReturn(checker);
    when(checker.fetchLastActivity(bad)).thenThrow(new RuntimeException("404"));
    when(checker.fetchLastActivity(good)).thenReturn(FRESH);
    when(checker.fetchDetails(good)).thenReturn(new UpdateDetails("t", "a", FRESH, "p"));
    when(repository.findSubscribersWithMode(good))
        .thenReturn(List.of(new SubscriptionRepository.SubscriberMode(10L, "INSTANT")));

    scheduler.poll();

    verify(notificationSender).send(any());
    verify(polling).updateCheckedAt(2L, FRESH);
  }

  @Test
  void poll_multipleBatches_paginatesUntilEmpty() {
    URI u1 = URI.create("https://github.com/a/b");
    URI u2 = URI.create("https://github.com/c/d");
    AppProperties.Scheduler sched = mock(AppProperties.Scheduler.class);
    when(sched.batchSize()).thenReturn(2);
    when(properties.scheduler()).thenReturn(sched);
    when(polling.findBatch(any(Cursor.class), any(Instant.class), eq(2)))
        .thenReturn(List.of(link(1L, u1, SEEN), link(2L, u2, SEEN)))
        .thenReturn(List.of());
    when(router.route(any())).thenReturn(checker);
    when(checker.fetchLastActivity(any())).thenReturn(FRESH);
    when(checker.fetchDetails(any())).thenReturn(new UpdateDetails("t", "a", FRESH, "p"));
    when(repository.findSubscribersWithMode(any()))
        .thenReturn(List.of(new SubscriptionRepository.SubscriberMode(10L, "INSTANT")));

    scheduler.poll();

    verify(polling, times(2)).findBatch(any(Cursor.class), any(Instant.class), eq(2));
    verify(notificationSender, times(2)).send(any());
  }

  @Test
  void recordsScrapeDurationTimerWithTypeTag() {
    URI url = URI.create("https://github.com/octocat/Hello-World");
    Link link = link(1L, url, SEEN);

    lenient().when(router.route(url)).thenReturn(checker);
    lenient().when(checker.type()).thenReturn("github");
    when(checker.fetchLastActivity(url)).thenReturn(SEEN); // обновлений нет — короткий путь
    when(polling.findBatch(any(), any(), anyInt())).thenReturn(List.of(link)).thenReturn(List.of());

    scheduler.poll();

    Timer timer = registry.get("scrapper.scrape.duration").tag("type", "github").timer();
    assertThat(timer.count()).isEqualTo(1);
  }

  @Test
  void recordsScrapeDurationTimerEvenWhenFetchFails() {
    URI url = URI.create("https://github.com/octocat/Hello-World");
    Link link = link(1L, url, SEEN);

    lenient().when(router.route(url)).thenReturn(checker);
    lenient().when(checker.type()).thenReturn("github");
    when(checker.fetchLastActivity(url)).thenThrow(new RuntimeException("API недоступен"));
    when(polling.findBatch(any(), any(), anyInt())).thenReturn(List.of(link)).thenReturn(List.of());

    scheduler.poll(); // checkOneGuarded ловит RuntimeException — poll() не должен упасть

    Timer timer = registry.get("scrapper.scrape.duration").tag("type", "github").timer();
    assertThat(timer.count()).isEqualTo(1);
  }

  @Test
  void recordsSeparateTimersPerType() {
    URI githubUrl = URI.create("https://github.com/octocat/Hello-World");
    URI soUrl = URI.create("https://stackoverflow.com/questions/1");
    Link githubLink = link(1L, githubUrl, SEEN);
    Link soLink = link(2L, soUrl, SEEN);

    UpdateChecker githubChecker = mock(UpdateChecker.class);
    UpdateChecker soChecker = mock(UpdateChecker.class);
    lenient().when(githubChecker.type()).thenReturn("github");
    lenient().when(soChecker.type()).thenReturn("stackoverflow");
    lenient().when(githubChecker.fetchLastActivity(githubUrl)).thenReturn(SEEN);
    lenient().when(soChecker.fetchLastActivity(soUrl)).thenReturn(SEEN);
    lenient().when(router.route(githubUrl)).thenReturn(githubChecker);
    lenient().when(router.route(soUrl)).thenReturn(soChecker);

    when(polling.findBatch(any(), any(), anyInt()))
        .thenReturn(List.of(githubLink, soLink))
        .thenReturn(List.of());

    scheduler.poll();

    assertThat(registry.get("scrapper.scrape.duration").tag("type", "github").timer().count())
        .isEqualTo(1);
    assertThat(
            registry.get("scrapper.scrape.duration").tag("type", "stackoverflow").timer().count())
        .isEqualTo(1);
  }
}
