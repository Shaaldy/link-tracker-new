package by.shaaldy.scrapper.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import by.shaaldy.scrapper.domain.Link;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JDBC-реализация батчевого обхода. Бин создаётся {@code SqlAccessConfig} при access-type=SQL.
 *
 * <p>TODO(stage-2): keyset по (last_checked_at, id) через кортежное сравнение {@code
 * (last_checked_at, id) > (:ts, :id) ORDER BY last_checked_at, id LIMIT :limit}.
 */
@Slf4j
@RequiredArgsConstructor
public class SqlLinkPollingRepository implements LinkPollingRepository {

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public List<Link> findBatch(Cursor cursor, int limit) {
    throw new UnsupportedOperationException("TODO(stage-2): SQL findBatch (keyset)");
  }

  @Override
  public void updateCheckedAt(long linkId, Instant checkedAt) {
    throw new UnsupportedOperationException("TODO(stage-2): SQL updateCheckedAt");
  }
}
