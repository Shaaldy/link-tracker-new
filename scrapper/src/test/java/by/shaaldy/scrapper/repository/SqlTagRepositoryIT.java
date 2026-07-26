package by.shaaldy.scrapper.repository;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.access-type=SQL")
class SqlTagRepositoryIT extends AbstractTagRepositoryIT {

  @Override
  protected long baseChatId() {
    return 7000L;
  }
}
