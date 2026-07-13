package by.shaaldy.scrapper.repository.orm.jpa;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import by.shaaldy.scrapper.repository.orm.jpa.entity.LinkEntity;

public interface LinkJpaRepository extends JpaRepository<LinkEntity, Long> {

  Optional<LinkEntity> findByUrl(String url);

  @Query(
      """
          SELECT l FROM LinkEntity l
          WHERE (l.lastCheckedAt > :ts OR (l.lastCheckedAt = :ts AND l.id > :id))
            AND l.lastCheckedAt < :tickStart
          ORDER BY l.lastCheckedAt, l.id
          """)
  List<LinkEntity> findKeysetBatch(
      @Param("ts") OffsetDateTime cursorTs,
      @Param("id") long cursorId,
      @Param("tickStart") OffsetDateTime tickStart,
      Limit limit);
}
