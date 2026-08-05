package by.shaaldy.scrapper.repository.orm;

import java.net.URI;

import org.springframework.test.context.TestPropertySource;

import by.shaaldy.scrapper.repository.AbstractSubscriptionRepositoryIT;

@TestPropertySource(properties = "app.access-type=ORM")
class OrmSubscriptionRepositoryIT extends AbstractSubscriptionRepositoryIT {

  @Override
  protected long baseChatId() {
    return 1000L;
  }

  @Override
  protected URI url() {
    return URI.create("https://github.com/orm/repo");
  }
}
