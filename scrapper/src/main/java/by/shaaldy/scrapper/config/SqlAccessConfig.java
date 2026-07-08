package by.shaaldy.scrapper.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import by.shaaldy.scrapper.repository.SqlLinkPollingRepository;
import by.shaaldy.scrapper.repository.SqlSubscriptionRepository;

/**
 * Включает весь набор JDBC-реализаций доступа к данным при {@code app.access-type=SQL} (а также
 * когда property не задана — SQL является провайдером по умолчанию).
 *
 * <p>Условие стоит на всей группе бинов, а не на каждом классе: провайдер выбирается атомарно —
 * либо весь SQL-набор, либо весь ORM-набор, промежуточных состояний нет.
 *
 * <p>TODO(stage-2): ORM-провайдер (OrmAccessConfig, havingValue="ORM") ещё не введён. До этого
 * момента доступен только access-type=SQL; при access-type=ORM ни один SubscriptionRepository не
 * поднимется и контекст упадёт.
 */
@Configuration
@ConditionalOnProperty(
    prefix = "app",
    name = "access-type",
    havingValue = "SQL",
    matchIfMissing = true)
class SqlAccessConfig {

  @Bean
  SqlSubscriptionRepository sqlSubscriptionRepository(NamedParameterJdbcTemplate jdbc) {
    return new SqlSubscriptionRepository(jdbc);
  }

  @Bean
  SqlLinkPollingRepository sqlLinkPollingRepository(NamedParameterJdbcTemplate jdbc) {
    return new SqlLinkPollingRepository(jdbc);
  }
}
