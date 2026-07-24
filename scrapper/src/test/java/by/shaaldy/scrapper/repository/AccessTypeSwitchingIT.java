package by.shaaldy.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import by.shaaldy.scrapper.repository.orm.OrmSubscriptionRepository;
import by.shaaldy.scrapper.repository.sql.SqlSubscriptionRepository;

class AccessTypeSwitchingIT {

  @Nested
  @TestPropertySource(properties = "app.access-type=SQL")
  class SqlProvider extends AbstractPostgresIT {

    @Autowired SubscriptionRepository repository;

    @Test
    void sqlAccessType_activatesSqlImplementation() {
      assertThat(repository).isInstanceOf(SqlSubscriptionRepository.class);
    }
  }

  @Nested
  @TestPropertySource(properties = "app.access-type=ORM")
  class OrmProvider extends AbstractPostgresIT {

    @Autowired SubscriptionRepository repository;

    @Test
    void ormAccessType_activatesOrmImplementation() {
      assertThat(repository).isInstanceOf(OrmSubscriptionRepository.class);
    }
  }
}
