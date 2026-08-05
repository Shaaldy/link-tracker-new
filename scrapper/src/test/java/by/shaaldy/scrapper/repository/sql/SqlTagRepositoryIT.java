package by.shaaldy.scrapper.repository.sql;

import org.springframework.test.context.TestPropertySource;

import by.shaaldy.scrapper.repository.AbstractTagRepositoryIT;

@TestPropertySource(properties = "app.access-type=SQL")
class SqlTagRepositoryIT extends AbstractTagRepositoryIT {

  @Override
  protected long baseChatId() {
    return 7000L;
  }
}
