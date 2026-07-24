package by.shaaldy.scrapper.client.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import by.shaaldy.scrapper.domain.UpdateDetails;

@ExtendWith(MockitoExtension.class)
class GitHubClientDetailsTest {

  @Mock GitHubApi api;
  @InjectMocks GitHubClient client;

  private static final URI URL = URI.create("https://github.com/owner/repo");
  private static final Instant OLD = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant MID = Instant.parse("2026-06-01T00:00:00Z");
  private static final Instant NEW = Instant.parse("2026-07-01T00:00:00Z");

  private GitHubItemResponse item(String title, String login, Instant updated, String body) {
    return new GitHubItemResponse(title, new GitHubItemResponse.User(login), OLD, updated, body);
  }

  private GitHubRepoResponse repo(Instant pushed) {
    return new GitHubRepoResponse(
        pushed, pushed, "owner/repo", new GitHubRepoResponse.Owner("owner"), "repo desc");
  }

  @Test
  void fetchDetails_prIsFreshest_returnsPrDetails() {
    when(api.getRepository("owner", "repo")).thenReturn(repo(OLD));
    when(api.getPulls("owner", "repo"))
        .thenReturn(List.of(item("Fix bug", "alice", NEW, "PR body")));
    when(api.getIssues("owner", "repo"))
        .thenReturn(List.of(item("Some issue", "bob", MID, "Issue body")));

    UpdateDetails d = client.fetchDetails(URL);

    assertThat(d.title()).isEqualTo("Fix bug");
    assertThat(d.author()).isEqualTo("alice");
    assertThat(d.preview()).isEqualTo("PR body");
  }

  @Test
  void fetchDetails_issueIsFreshest_returnsIssueDetails() {
    when(api.getRepository("owner", "repo")).thenReturn(repo(OLD));
    when(api.getPulls("owner", "repo"))
        .thenReturn(List.of(item("Old PR", "alice", OLD, "PR body")));
    when(api.getIssues("owner", "repo"))
        .thenReturn(List.of(item("New issue", "bob", NEW, "Issue body")));

    UpdateDetails d = client.fetchDetails(URL);

    assertThat(d.title()).isEqualTo("New issue");
    assertThat(d.author()).isEqualTo("bob");
  }

  @Test
  void fetchDetails_repoPushIsFreshest_returnsRepoDetails() {
    // Общий случай: пуш в код свежее любых PR/Issue.
    when(api.getRepository("owner", "repo")).thenReturn(repo(NEW));
    when(api.getPulls("owner", "repo"))
        .thenReturn(List.of(item("Old PR", "alice", OLD, "PR body")));
    when(api.getIssues("owner", "repo"))
        .thenReturn(List.of(item("Old issue", "bob", MID, "Issue body")));

    UpdateDetails d = client.fetchDetails(URL);

    assertThat(d.title()).isEqualTo("owner/repo");
    assertThat(d.preview()).isEqualTo("repo desc");
  }

  @Test
  void fetchDetails_noPrNoIssue_fallsBackToRepo() {
    when(api.getRepository("owner", "repo")).thenReturn(repo(MID));
    when(api.getPulls("owner", "repo")).thenReturn(List.of());
    when(api.getIssues("owner", "repo")).thenReturn(List.of());

    UpdateDetails d = client.fetchDetails(URL);

    assertThat(d.title()).isEqualTo("owner/repo");
  }
}
