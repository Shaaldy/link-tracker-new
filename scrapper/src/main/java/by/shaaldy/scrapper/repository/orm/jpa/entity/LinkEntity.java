package by.shaaldy.scrapper.repository.orm.jpa.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity таблицы {@code links}. Внутренняя деталь ORM-реализации — наружу (в интерфейс репозитория)
 * не течёт, конвертируется в доменный Link/TrackedLink на границе обёртки.
 *
 * <p>Время хранится в {@link OffsetDateTime}: драйвер PostgreSQL надёжно маппит timestamptz в
 * OffsetDateTime, но не в Instant (та же причина, что в SQL-ветке). В домен конвертируем
 * OffsetDateTime → Instant в обёртке.
 */
@Entity
@Table(name = "links")
@Getter
@Setter
@NoArgsConstructor
public class LinkEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String url;

  // insertable=false: при вставке новой ссылки метку ставит БД (DEFAULT эпохи), как и в
  // SQL-ветке (INSERT INTO links (url) без метки) — обе реализации берут дефолт из БД.
  // updatable (по умолчанию true) — планировщик двигает метку через UPDATE / dirty-checking.
  @Column(name = "last_checked_at", nullable = false, insertable = false)
  private OffsetDateTime lastCheckedAt;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private OffsetDateTime createdAt;
}
