package by.shaaldy.scrapper.repository.sql;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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

  @Override
  public List<TrackedLink> findLinksByChatAndTag(long chatId, String tag) {
    // Переиспользуем агрегацию тегов/фильтров, добавив фильтр: только подписки, где есть :tag.
    String sql =
        """
            SELECT l.id, l.url,
                   ARRAY(SELECT lt.tag FROM link_tags lt
                         WHERE lt.chat_id = cl.chat_id AND lt.link_id = cl.link_id) AS tags,
                   ARRAY(SELECT lf.filter FROM link_filters lf
                         WHERE lf.chat_id = cl.chat_id AND lf.link_id = cl.link_id) AS filters
            FROM chat_links cl
            JOIN links l ON l.id = cl.link_id
            WHERE cl.chat_id = :chatId
              AND EXISTS (SELECT 1 FROM link_tags t
                          WHERE t.chat_id = cl.chat_id AND t.link_id = cl.link_id AND t.tag = :tag)
            """;
    return jdbc.query(
        sql, Map.of("chatId", chatId, "tag", tag), SqlSubscriptionRepository::mapTrackedLink);
  }

  @Override
  public Set<String> findTagsByChat(long chatId) {
    String sql = "SELECT DISTINCT tag FROM link_tags WHERE chat_id = :chatId";
    List<String> tags = jdbc.queryForList(sql, Map.of("chatId", chatId), String.class);
    return new HashSet<>(tags);
  }

  @Override
  public boolean addTag(long chatId, URI url, String tag) {
    String sql =
        """
            INSERT INTO link_tags (chat_id, link_id, tag)
            SELECT :chatId, l.id, :tag
            FROM links l
            WHERE l.url = :url
              AND EXISTS (SELECT 1 FROM chat_links cl
                          WHERE cl.chat_id = :chatId AND cl.link_id = l.id)
            ON CONFLICT DO NOTHING
            """;
    int rows = jdbc.update(sql, Map.of("chatId", chatId, "url", url.toString(), "tag", tag));
    return rows > 0;
  }

  @Override
  public boolean removeTag(long chatId, URI url, String tag) {
    String sql =
        """
            DELETE FROM link_tags
            WHERE chat_id = :chatId
              AND tag = :tag
              AND link_id = (SELECT id FROM links WHERE url = :url)
            """;
    int rows = jdbc.update(sql, Map.of("chatId", chatId, "url", url.toString(), "tag", tag));
    return rows > 0;
  }

  /** Разворачивает строку с PG-массивами tags/filters в доменный TrackedLink. */
  private static TrackedLink mapTrackedLink(ResultSet rs, int rowNum) throws SQLException {
    long id = rs.getLong("id");
    URI url = URI.create(rs.getString("url"));
    List<String> tags = List.of((String[]) rs.getArray("tags").getArray());
    List<String> filters = List.of((String[]) rs.getArray("filters").getArray());
    return new TrackedLink(id, url, tags, filters);
  }

  @Override
  public boolean updateNotificationMode(long chatId, String mode, Integer digestHour) {
    String sql =
        """
                    UPDATE chats
                    SET notification_mode = :mode,
                        digest_hour = :digestHour
                    WHERE chat_id = :chatId
                    """;
    Map<String, Object> params = new HashMap<>();
    params.put("chatId", chatId);
    params.put("mode", mode);
    params.put(
        "digestHour", digestHour); // null при INSTANT — NamedParameterJdbcTemplate кладёт NULL
    int rows = jdbc.update(sql, params);
    return rows > 0;
  }

  @Override
  public Set<Long> findDigestRecipients(int hour) {
    String sql =
        """
                    SELECT chat_id
                    FROM chats
                    WHERE notification_mode = 'DIGEST'
                      AND digest_hour = :hour
                    """;
    List<Long> ids = jdbc.queryForList(sql, Map.of("hour", hour), Long.class);
    return new HashSet<>(ids);
  }
}
