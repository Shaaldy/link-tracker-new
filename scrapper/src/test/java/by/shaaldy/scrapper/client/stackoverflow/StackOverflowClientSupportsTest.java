package by.shaaldy.scrapper.client.stackoverflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StackOverflowClientSupportsTest {

  private final StackOverflowClient client =
      new StackOverflowClient(null, null); // Api и Properties в supports() не используются

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://stackoverflow.com/questions/123",
        "https://stackoverflow.com/questions/123/how-to-do-x",
        "https://stackoverflow.com/q/123",
        "https://stackoverflow.com/questions/123456789"
      })
  void supports_questionUrl_returnsTrue(String url) {
    assertThat(client.supports(URI.create(url))).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://stackoverflow.com/questions/abc", // id не число
        "https://stackoverflow.com/questions/", // нет id
        "https://stackoverflow.com/tags/java", // не вопрос
        "https://superuser.com/questions/123", // чужой сайт сети
        "https://github.com/a/b"
      })
  void supports_nonQuestionUrl_returnsFalse(String url) {
    assertThat(client.supports(URI.create(url))).isFalse();
  }
}
