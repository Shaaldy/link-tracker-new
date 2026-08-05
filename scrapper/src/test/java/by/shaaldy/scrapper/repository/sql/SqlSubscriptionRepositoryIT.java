package by.shaaldy.scrapper.repository.sql;

import java.net.URI;

import org.springframework.test.context.TestPropertySource;

import by.shaaldy.scrapper.repository.AbstractSubscriptionRepositoryIT;

@TestPropertySource(properties = "app.access-type=SQL")
class SqlSubscriptionRepositoryIT extends AbstractSubscriptionRepositoryIT {

  @Override
  protected long baseChatId() {
    return 0L;
  }

  @Override
  protected URI url() {
    return URI.create("https://github.com/a/b");
  }
}
