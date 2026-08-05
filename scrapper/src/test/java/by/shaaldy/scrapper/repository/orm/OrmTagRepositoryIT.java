package by.shaaldy.scrapper.repository.orm;

import org.springframework.test.context.TestPropertySource;

import by.shaaldy.scrapper.repository.AbstractTagRepositoryIT;

@TestPropertySource(properties = "app.access-type=ORM")
class OrmTagRepositoryIT extends AbstractTagRepositoryIT {

  @Override
  protected long baseChatId() {
    return 8000L;
  }
}
