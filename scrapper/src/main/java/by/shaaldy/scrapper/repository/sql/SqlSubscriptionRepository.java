package by.shaaldy.scrapper.repository.sql;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.annotation.Transactional;

import by.shaaldy.scrapper.domain.TrackedLink;
import by.shaaldy.scrapper.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JDBC-реализация CRUD подписок. Бин создаётся {@code SqlAccessConfig} при access-type=SQL. НЕ
 * помечена @Repository — регистрируется через @Bean, чтобы условие жило в одном конфиге.
 *
 * <p>Возвращает только доменные типы (TrackedLink) — строки БД не текут наружу. Теги/фильтры
 * читаются агрегацией в PG-массивы, пишутся батчем.
 */
@Slf4j
@RequiredArgsConstructor
public class SqlSubscriptionRepository implements SubscriptionRepository {

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public boolean registerChat(long chatId) {
    String sql =
        """
                INSERT INTO chats (chat_id)
                VALUES (:chatId)
                ON CONFLICT (chat_id) DO NOTHING
                """;
    int rows = jdbc.update(sql, Map.of("chatId", chatId));
    return rows > 0;
  }

  @Override
  public boolean removeChat(long chatId) {
    // Каскад (ON DELETE CASCADE) сам снесёт chat_links, link_tags, link_filters.
    String sql = "DELETE FROM chats WHERE chat_id = :chatId";
    int rows = jdbc.update(sql, Map.of("chatId", chatId));
    return rows > 0;
  }

  @Override
  public boolean chatExists(long chatId) {
    String sql = "SELECT EXISTS(SELECT 1 FROM chats WHERE chat_id = :chatId)";
    return Boolean.TRUE.equals(jdbc.queryForObject(sql, Map.of("chatId", chatId), Boolean.class));
  }

  @Override
  @Transactional
  public TrackedLink addLink(long chatId, URI url, List<String> tags, List<String> filters) {
    long linkId = insertOrGetLinkId(url);
    insertSubscription(chatId, linkId);
    insertAttributes("link_tags", "tag", chatId, linkId, tags);
    insertAttributes("link_filters", "filter", chatId, linkId, filters);
    return new TrackedLink(linkId, url, tags, filters);
  }

  /** Вставляет ссылку или возвращает id уже существующей (ссылка глобально уникальна по url). */
  private long insertOrGetLinkId(URI url) {
    String insert =
        """
                INSERT INTO links (url)
                VALUES (:url)
                ON CONFLICT (url) DO NOTHING
                RETURNING id
                """;
    List<Long> inserted = jdbc.queryForList(insert, Map.of("url", url.toString()), Long.class);
    if (!inserted.isEmpty()) {
      return inserted.getFirst();
    }
    String select = "SELECT id FROM links WHERE url = :url";
    return jdbc.queryForObject(select, Map.of("url", url.toString()), Long.class);
  }

  private void insertSubscription(long chatId, long linkId) {
    String sql =
        """
                INSERT INTO chat_links (chat_id, link_id)
                VALUES (:chatId, :linkId)
                ON CONFLICT (chat_id, link_id) DO NOTHING
                """;
    jdbc.update(sql, Map.of("chatId", chatId, "linkId", linkId));
  }

  /** Батч-вставка тегов/фильтров подписки; для пустого списка — no-op. */
  private void insertAttributes(
      String table, String column, long chatId, long linkId, List<String> values) {
    if (values.isEmpty()) {
      return;
    }
    String sql =
        "INSERT INTO %s (chat_id, link_id, %s) VALUES (:chatId, :linkId, :value)"
            .formatted(table, column);
    SqlParameterSource[] batch =
        values.stream()
            .map(
                v ->
                    new MapSqlParameterSource()
                        .addValue("chatId", chatId)
                        .addValue("linkId", linkId)
                        .addValue("value", v))
            .toArray(SqlParameterSource[]::new);
    jdbc.batchUpdate(sql, batch);
  }

  @Override
  @Transactional
  public boolean removeLink(long chatId, URI url) {
    String deleteSubscription =
        """
                DELETE FROM chat_links
                WHERE chat_id = :chatId
                  AND link_id = (SELECT id FROM links WHERE url = :url)
                """;
    int rows = jdbc.update(deleteSubscription, Map.of("chatId", chatId, "url", url.toString()));
    if (rows == 0) {
      return false;
    }
    String deleteOrphan =
        """
                DELETE FROM links
                WHERE url = :url
                  AND NOT EXISTS (SELECT 1 FROM chat_links WHERE link_id = links.id)
                """;
    jdbc.update(deleteOrphan, Map.of("url", url.toString()));
    return true;
  }

  @Override
  public boolean subscriptionExists(long chatId, URI url) {
    String sql =
        """
                SELECT EXISTS(
                  SELECT 1 FROM chat_links
                  WHERE chat_id = :chatId
                    AND link_id = (SELECT id FROM links WHERE url = :url)
                )
                """;
    return Boolean.TRUE.equals(
        jdbc.queryForObject(sql, Map.of("chatId", chatId, "url", url.toString()), Boolean.class));
  }

  @Override
  public List<TrackedLink> findLinksByChat(long chatId) {
    String sql =
        """
                SELECT
                  l.id,
                  l.url,
                  ARRAY(SELECT tag FROM link_tags
                        WHERE chat_id = cl.chat_id AND link_id = cl.link_id) AS tags,
                  ARRAY(SELECT filter FROM link_filters
                        WHERE chat_id = cl.chat_id AND link_id = cl.link_id) AS filters
                FROM chat_links cl
                JOIN links l ON l.id = cl.link_id
                WHERE cl.chat_id = :chatId
                """;
    return jdbc.query(sql, Map.of("chatId", chatId), SqlSubscriptionRepository::mapTrackedLink);
  }

  @Override
  public Set<Long> findSubscribers(URI url) {
    String sql =
        """
                SELECT chat_id FROM chat_links
                WHERE link_id = (SELECT id FROM links WHERE url = :url)
                """;
    return Set.copyOf(jdbc.queryForList(sql, Map.of("url", url.toString()), Long.class));
  }

  @Override
  public void deleteAll() {
     jdbc.getJdbcTemplate()
        .execute("TRUNCATE chats, links, chat_links, link_tags, link_filters CASCADE");
  }

  /** Разворачивает строку с PG-массивами tags/filters в доменный TrackedLink. */
  private static TrackedLink mapTrackedLink(ResultSet rs, int rowNum) throws SQLException {
    long id = rs.getLong("id");
    URI url = URI.create(rs.getString("url"));
    List<String> tags = List.of((String[]) rs.getArray("tags").getArray());
    List<String> filters = List.of((String[]) rs.getArray("filters").getArray());
    return new TrackedLink(id, url, tags, filters);
  }
}
