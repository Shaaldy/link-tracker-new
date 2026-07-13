package by.shaaldy.scrapper.client.github;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import by.shaaldy.scrapper.client.UpdateChecker;
import by.shaaldy.scrapper.domain.UpdateDetails;
import by.shaaldy.scrapper.util.TextPreview;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GitHubClient implements UpdateChecker {

  private static final Pattern PATH = Pattern.compile("^/[^/]+/[^/]+(/.*)?$");

  private final GitHubApi api;

  @Override
  public boolean supports(URI url) {
    return "github.com".equals(url.getHost()) && PATH.matcher(url.getPath()).matches();
  }

  @Override
  public Instant fetchLastActivity(URI url) {
    String[] parts = url.getPath().split("/"); // ["", owner, repo, ...]
    return api.getRepository(parts[1], parts[2]).pushedAt();
  }

  @Override
  public UpdateDetails fetchDetails(URI url) {
    String[] parts = url.getPath().split("/");
    String owner = parts[1];
    String repo = parts[2];

    // Берём свежайший из последнего PR и последнего Issue (у обоих sort=updated desc, per_page=1).
    GitHubItemResponse latest =
        Stream.concat(api.getPulls(owner, repo).stream(), api.getIssues(owner, repo).stream())
            .max(Comparator.comparing(GitHubItemResponse::updatedAt))
            .orElse(null);
    if (latest == null) {
      return new UpdateDetails(null, null, null, null);
    }
    String author = latest.user() == null ? null : latest.user().login();
    return new UpdateDetails(
        latest.title(), author, latest.createdAt(), TextPreview.preview(latest.body()));
  }
}
