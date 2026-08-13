package by.shaaldy.scrapper.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import by.shaaldy.scrapper.client.LinkSourceRouter;
import by.shaaldy.scrapper.repository.orm.OrmLinkMetricsRepository;
import by.shaaldy.scrapper.repository.orm.OrmLinkPollingRepository;
import by.shaaldy.scrapper.repository.orm.OrmSubscriptionRepository;
import by.shaaldy.scrapper.repository.orm.jpa.ChatJpaRepository;
import by.shaaldy.scrapper.repository.orm.jpa.ChatLinkJpaRepository;
import by.shaaldy.scrapper.repository.orm.jpa.LinkJpaRepository;

/**
 * Включает весь набор JPA/Hibernate-реализаций при {@code app.access-type=ORM}.
 *
 * <p>Зеркало {@code SqlAccessConfig}. Здесь НЕТ {@code matchIfMissing}: ORM поднимается только при
 * явно заданном ORM; дефолт (отсутствие property) закреплён за SQL. Иначе при незаданной property
 * поднялись бы оба провайдера и контекст упал бы на дубле бинов SubscriptionRepository.
 */
@Configuration
@ConditionalOnProperty(prefix = "app", name = "access-type", havingValue = "ORM")
class OrmAccessConfig {

  @Bean
  OrmSubscriptionRepository ormSubscriptionRepository(
      ChatJpaRepository chats, LinkJpaRepository links, ChatLinkJpaRepository chatLinks) {
    return new OrmSubscriptionRepository(chats, links, chatLinks);
  }

  @Bean
  OrmLinkPollingRepository ormLinkPollingRepository(LinkJpaRepository links) {
    return new OrmLinkPollingRepository(links);
  }

  @Bean
  OrmLinkMetricsRepository ormLinkMetricsRepository(
      LinkJpaRepository links, LinkSourceRouter router) {
    return new OrmLinkMetricsRepository(links, router);
  }
}
