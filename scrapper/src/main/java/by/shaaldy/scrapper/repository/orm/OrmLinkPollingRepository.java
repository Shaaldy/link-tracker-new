package by.shaaldy.scrapper.repository.orm;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.transaction.annotation.Transactional;

import by.shaaldy.scrapper.domain.Link;
import by.shaaldy.scrapper.repository.LinkPollingRepository;
import by.shaaldy.scrapper.repository.orm.jpa.LinkJpaRepository;
import by.shaaldy.scrapper.repository.orm.jpa.entity.LinkEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA/Hibernate-реализация батчевого обхода. Бин создаётся {@code OrmAccessConfig} при
 * access-type=ORM. Keyset реализован в {@link LinkJpaRepository#findKeysetBatch} через HQL (кортеж
 * развёрнут в OR). Entity → домен маппится на границе.
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
public class OrmLinkPollingRepository implements LinkPollingRepository {

  private final LinkJpaRepository links;

  @Override
  public List<Link> findBatch(Cursor cursor, Instant tickStart, int limit) {
    OffsetDateTime cursorTs = OffsetDateTime.ofInstant(cursor.lastCheckedAt(), ZoneOffset.UTC);
    OffsetDateTime tickTs = OffsetDateTime.ofInstant(tickStart, ZoneOffset.UTC);
    return links.findKeysetBatch(cursorTs, cursor.id(), tickTs, Limit.of(limit)).stream()
        .map(OrmLinkPollingRepository::toDomain)
        .toList();
  }

  @Override
  public void updateCheckedAt(long linkId, Instant checkedAt) {
    // Dirty-checking: загружаем managed entity, меняем поле — Hibernate сам сгенерит UPDATE
    // на коммите транзакции, без явного save().
    links
        .findById(linkId)
        .ifPresent(
            entity -> entity.setLastCheckedAt(OffsetDateTime.ofInstant(checkedAt, ZoneOffset.UTC)));
  }

  private static Link toDomain(LinkEntity e) {
    return Link.builder()
        .id(e.getId())
        .url(URI.create(e.getUrl()))
        .lastCheckedAt(e.getLastCheckedAt() == null ? null : e.getLastCheckedAt().toInstant())
        .createdAt(e.getCreatedAt() == null ? null : e.getCreatedAt().toInstant())
        .build();
  }
}
