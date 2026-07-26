package by.shaaldy.scrapper.repository;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.access-type=ORM")
class OrmTagRepositoryIT extends AbstractTagRepositoryIT {

  @Override
  protected long baseChatId() {
    return 8000L;
  }
}
