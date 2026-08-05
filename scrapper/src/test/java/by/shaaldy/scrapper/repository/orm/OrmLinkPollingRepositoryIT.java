package by.shaaldy.scrapper.repository.orm;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import by.shaaldy.scrapper.domain.Link;
import by.shaaldy.scrapper.repository.AbstractPostgresIT;
import by.shaaldy.scrapper.repository.LinkPollingRepository;
import by.shaaldy.scrapper.repository.LinkPollingRepository.Cursor;
import by.shaaldy.scrapper.repository.SubscriptionRepository;

@TestPropertySource(properties = "app.access-type=ORM")
class OrmLinkPollingRepositoryIT extends AbstractPostgresIT {

  @Autowired SubscriptionRepository subscriptions;
  @Autowired LinkPollingRepository polling;

  private static final Instant FAR_FUTURE = Instant.parse("2030-01-01T00:00:00Z");

  private long insertLink(long chatId, String url) {
    subscriptions.registerChat(chatId);
    return subscriptions.addLink(chatId, URI.create(url), List.of(), List.of()).id();
  }

  @Test
  void findBatch_ordersByLastCheckedThenId() {
    long a = insertLink(6001L, "https://github.com/orm/a");
    long b = insertLink(6001L, "https://github.com/orm/b");
    long c = insertLink(6001L, "https://github.com/orm/c");
    polling.updateCheckedAt(b, Instant.parse("2026-03-01T00:00:00Z"));
    polling.updateCheckedAt(c, Instant.parse("2026-06-01T00:00:00Z"));

    List<Link> batch = polling.findBatch(Cursor.start(), FAR_FUTURE, 10);

    List<Long> ids = batch.stream().map(Link::getId).toList();
    assertThat(ids).containsSubsequence(a, b, c);
  }

  @Test
  void findBatch_keysetShift_secondBatchDoesNotRepeatFirst() {
    long a = insertLink(6002L, "https://github.com/orm/d");
    long b = insertLink(6002L, "https://github.com/orm/e");
    long c = insertLink(6002L, "https://github.com/orm/f");
    polling.updateCheckedAt(a, Instant.parse("2026-01-01T00:00:00Z"));
    polling.updateCheckedAt(b, Instant.parse("2026-02-01T00:00:00Z"));
    polling.updateCheckedAt(c, Instant.parse("2026-03-01T00:00:00Z"));

    List<Link> first = polling.findBatch(Cursor.start(), FAR_FUTURE, 2);
    assertThat(first).hasSize(2);

    Link last = first.getLast();
    Cursor cursor = new Cursor(last.getLastCheckedAt(), last.getId());
    List<Link> second = polling.findBatch(cursor, FAR_FUTURE, 2);

    List<Long> firstIds = first.stream().map(Link::getId).toList();
    List<Long> secondIds = second.stream().map(Link::getId).toList();
    assertThat(secondIds).doesNotContainAnyElementsOf(firstIds);
  }

  @Test
  void findBatch_tickStart_excludesLinksCheckedAtOrAfter() {
    long old = insertLink(6003L, "https://github.com/orm/g");
    long recent = insertLink(6003L, "https://github.com/orm/h");
    Instant tickStart = Instant.parse("2026-05-01T00:00:00Z");
    polling.updateCheckedAt(old, Instant.parse("2026-01-01T00:00:00Z"));
    polling.updateCheckedAt(recent, Instant.parse("2026-09-01T00:00:00Z"));

    List<Link> batch = polling.findBatch(Cursor.start(), tickStart, 10);

    List<Long> ids = batch.stream().map(Link::getId).toList();
    assertThat(ids).contains(old).doesNotContain(recent);
  }

  @Test
  void updateCheckedAt_movesWatermark() {
    long id = insertLink(6004L, "https://github.com/orm/i");
    Instant when = Instant.parse("2026-04-01T00:00:00Z");

    polling.updateCheckedAt(id, when);

    List<Link> batch = polling.findBatch(Cursor.start(), FAR_FUTURE, 100);
    Link found = batch.stream().filter(l -> l.getId() == id).findFirst().orElseThrow();
    assertThat(found.getLastCheckedAt()).isEqualTo(when);
  }
}
