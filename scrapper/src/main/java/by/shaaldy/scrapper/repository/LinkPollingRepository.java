package by.shaaldy.scrapper.repository;

import java.time.Instant;
import java.util.List;

import by.shaaldy.scrapper.domain.Link;

/**
 * Контракт обхода ссылок планировщиком. Отдельно от {@link SubscriptionRepository}: у них разные
 * клиенты (контроллеры vs планировщик) и разный характер операций (точечный CRUD vs батчевый
 * обход). Реализации — SqlLinkPollingRepository / OrmLinkPollingRepository.
 *
 * <p>Ни один метод не отдаёт наружу impl-специфичные типы (RowSet, Entity, Pageable) — только
 * доменный {@link Link} и примитивы.
 */
public interface LinkPollingRepository {

  /**
   * Следующий батч ссылок, отсортированных по {@code (last_checked_at, id)} по возрастанию — «самые
   * давно проверенные вперёд». Keyset-пагинация: возвращаются строки, идущие в этом порядке строго
   * после {@code cursor}. WHERE / ORDER BY / LIMIT выполняются в SQL, в память попадает только батч
   * (требование ДЗ: не грузить все ссылки разом).
   *
   * @param cursor позиция, после которой брать; {@link Cursor#start()} для первого батча
   * @param limit размер батча (app.scheduler.batch-size)
   * @return батч ссылок; пустой список — обход завершён
   */
  List<Link> findBatch(Cursor cursor, int limit);

  /** Продвинуть водяную метку по ссылке после успешной проверки. */
  void updateCheckedAt(long linkId, Instant checkedAt);

  /**
   * Позиция keyset-курсора по составному ключу сортировки {@code (last_checked_at, id)}. {@code id}
   * разрывает ничью, когда у нескольких ссылок совпадает {@code lastCheckedAt}.
   */
  record Cursor(Instant lastCheckedAt, long id) {

    /**
     * Начало обхода: на секунду раньше эпохи (дефолт last_checked_at в схеме) — гарантированно
     * раньше любой строки, поэтому строгое {@code >} в keyset захватит и новые ссылки с
     * меткой-эпохой.
     */
    public static Cursor start() {
      return new Cursor(Instant.EPOCH.minusSeconds(1), Long.MIN_VALUE);
    }
  }
}
