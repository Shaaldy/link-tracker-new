package by.shaaldy.scrapper.repository;

import java.util.Map;

/**
 * Узкий контракт для снятия метрик по ссылкам. Не связан с LinkPollingRepository: тот отдаёт батчи
 * для обхода планировщиком, этот — агрегат по всей таблице для наблюдаемости.
 */
public interface LinkMetricsRepository {

  /** Количество ссылок в таблице, сгруппированное по типу источника (github/stackoverflow). */
  Map<String, Long> countActiveByType();
}
