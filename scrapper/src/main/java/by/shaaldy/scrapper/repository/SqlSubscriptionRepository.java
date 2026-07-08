package by.shaaldy.scrapper.repository;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import by.shaaldy.scrapper.domain.TrackedLink;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JDBC-реализация CRUD подписок. Бин создаётся {@code SqlAccessConfig} при access-type=SQL. НЕ
 * помечена @Repository — регистрируется через @Bean, чтобы условие жило в одном конфиге.
 *
 * <p>TODO(stage-2): наполнить тела (NamedParameterJdbcTemplate, ON CONFLICT ... RETURNING id,
 * маппинг строк ↔ TrackedLink на границе).
 */
@Slf4j
@RequiredArgsConstructor
public class SqlSubscriptionRepository implements SubscriptionRepository {

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public boolean registerChat(long chatId) {
    throw new UnsupportedOperationException("TODO(stage-2): SQL registerChat");
  }

  @Override
  public boolean removeChat(long chatId) {
    throw new UnsupportedOperationException("TODO(stage-2): SQL removeChat");
  }

  @Override
  public boolean chatExists(long chatId) {
    throw new UnsupportedOperationException("TODO(stage-2): SQL chatExists");
  }

  @Override
  public TrackedLink addLink(long chatId, URI url, List<String> tags, List<String> filters) {
    throw new UnsupportedOperationException("TODO(stage-2): SQL addLink");
  }

  @Override
  public boolean removeLink(long chatId, URI url) {
    throw new UnsupportedOperationException("TODO(stage-2): SQL removeLink");
  }

  @Override
  public boolean subscriptionExists(long chatId, URI url) {
    throw new UnsupportedOperationException("TODO(stage-2): SQL subscriptionExists");
  }

  @Override
  public List<TrackedLink> findLinksByChat(long chatId) {
    throw new UnsupportedOperationException("TODO(stage-2): SQL findLinksByChat");
  }

  @Override
  public Set<Long> findSubscribers(URI url) {
    throw new UnsupportedOperationException("TODO(stage-2): SQL findSubscribers");
  }
}
