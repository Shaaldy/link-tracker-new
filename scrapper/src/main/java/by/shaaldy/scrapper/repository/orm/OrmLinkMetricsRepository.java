package by.shaaldy.scrapper.repository.orm;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import by.shaaldy.scrapper.client.LinkSourceRouter;
import by.shaaldy.scrapper.repository.LinkMetricsRepository;
import by.shaaldy.scrapper.repository.orm.jpa.LinkJpaRepository;
import by.shaaldy.scrapper.repository.orm.jpa.entity.LinkEntity;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrmLinkMetricsRepository implements LinkMetricsRepository {

  private final LinkJpaRepository links;
  private final LinkSourceRouter router;

  @Override
  public Map<String, Long> countActiveByType() {
    return links.findAll().stream()
        .map(LinkEntity::getUrl)
        .map(URI::create)
        .collect(Collectors.groupingBy(url -> router.route(url).type(), Collectors.counting()));
  }
}
