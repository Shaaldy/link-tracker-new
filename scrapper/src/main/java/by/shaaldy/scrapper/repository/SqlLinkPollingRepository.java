package by.shaaldy.scrapper.repository;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import by.shaaldy.scrapper.domain.Link;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JDBC-реализация батчевого обхода. Бин создаётся {@code SqlAccessConfig} при access-type=SQL.
 *
 * <p>Keyset-пагинация по составному ключу {@code (last_checked_at, id)} через кортежное сравнение.
 * Метки времени на границе идут через {@link OffsetDateTime} (UTC): драйвер PostgreSQL не умеет
 * конвертировать timestamptz напрямую в {@link Instant}, но поддерживает OffsetDateTime.
 */
@Slf4j
@RequiredArgsConstructor
public class SqlLinkPollingRepository implements LinkPollingRepository {

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public List<Link> findBatch(Cursor cursor, int limit) {
    boolean fromStart = cursor.equals(Cursor.start());

    // Первый батч — без keyset-условия (Instant.MIN не представим в timestamptz надёжно);
    // последующие — кортежное сравнение (last_checked_at, id) > (:ts, :id).
    String where = fromStart ? "" : "WHERE (l.last_checked_at, l.id) > (:cursorTs, :cursorId)";

    String sql =
        """
            SELECT l.id, l.url, l.last_checked_at, l.created_at
            FROM links l
            %s
            ORDER BY l.last_checked_at, l.id
            LIMIT :limit
            """
            .formatted(where);

    Map<String, Object> params = new HashMap<>();
    params.put("limit", limit);
    if (!fromStart) {
      params.put("cursorTs", OffsetDateTime.ofInstant(cursor.lastCheckedAt(), ZoneOffset.UTC));
      params.put("cursorId", cursor.id());
    }
    return jdbc.query(sql, params, SqlLinkPollingRepository::mapLink);
  }

  @Override
  public void updateCheckedAt(long linkId, Instant checkedAt) {
    String sql = "UPDATE links SET last_checked_at = :checkedAt WHERE id = :id";
    jdbc.update(
        sql,
        Map.of("id", linkId, "checkedAt", OffsetDateTime.ofInstant(checkedAt, ZoneOffset.UTC)));
  }

  private static Link mapLink(ResultSet rs, int rowNum) throws SQLException {
    return Link.builder()
        .id(rs.getLong("id"))
        .url(URI.create(rs.getString("url")))
        .lastCheckedAt(toInstant(rs, "last_checked_at"))
        .createdAt(toInstant(rs, "created_at"))
        .build();
  }

  /**
   * timestamptz -> Instant через OffsetDateTime (прямой getObject(Instant) драйвером не поддержан).
   */
  private static Instant toInstant(ResultSet rs, String column) throws SQLException {
    OffsetDateTime odt = rs.getObject(column, OffsetDateTime.class);
    return odt == null ? null : odt.toInstant();
  }
}
