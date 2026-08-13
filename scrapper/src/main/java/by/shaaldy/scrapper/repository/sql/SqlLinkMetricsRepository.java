package by.shaaldy.scrapper.repository.sql;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import by.shaaldy.scrapper.client.LinkSourceRouter;
import by.shaaldy.scrapper.repository.LinkMetricsRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SqlLinkMetricsRepository implements LinkMetricsRepository {

  private final NamedParameterJdbcTemplate jdbc;
  private final LinkSourceRouter router;

  @Override
  public Map<String, Long> countActiveByType() {
    List<String> urls = jdbc.queryForList("SELECT url FROM links", Map.of(), String.class);
    return urls.stream()
        .map(URI::create)
        .collect(Collectors.groupingBy(url -> router.route(url).type(), Collectors.counting()));
  }
}
