package by.shaaldy.scrapper.client.github;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GitHubClientSupportsTest {

  private final GitHubClient client =
      new GitHubClient(null, null, null); // GitHubApi в supports() не используется

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://github.com/octocat/Hello-World",
        "https://github.com/octocat/Hello-World/",
        "https://github.com/octocat/Hello-World/issues/1",
        "https://github.com/a/b"
      })
  void supports_repoUrl_returnsTrue(String url) {
    assertThat(client.supports(URI.create(url))).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://github.com/octocat", // нет repo
        "https://github.com/", // нет owner/repo
        "https://gitlab.com/a/b", // чужой хост
        "https://www.github.com/a/b", // www по контракту не матчим
        "https://stackoverflow.com/questions/1" // не тот источник
      })
  void supports_nonRepoUrl_returnsFalse(String url) {
    assertThat(client.supports(URI.create(url))).isFalse();
  }
}
