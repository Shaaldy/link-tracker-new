package by.shaaldy.scrapper.client.github;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import io.github.resilience4j.retry.RetryRegistry;
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
  private final RetryRegistry retryRegistry;

  @Override
  public boolean supports(URI url) {
    return "github.com".equals(url.getHost()) && PATH.matcher(url.getPath()).matches();
  }

  @Override
  public Instant fetchLastActivity(URI url) {
    String[] parts = url.getPath().split("/"); // ["", owner, repo, ...]
    return getRepository(parts[1], parts[2]).pushedAt();
  }

  @Override
  public UpdateDetails fetchDetails(URI url) {
    String[] parts = url.getPath().split("/");
    String owner = parts[1];
    String repo = parts[2];

    List<Candidate> candidates = new ArrayList<>();

    GitHubRepoResponse repository = getRepository(owner, repo);
    candidates.add(repoCandidate(repository));

    getPulls(owner, repo).stream().findFirst().map(GitHubClient::itemCandidate).ifPresent(candidates::add);
    getIssues(owner, repo).stream().findFirst().map(GitHubClient::itemCandidate).ifPresent(candidates::add);

    return candidates.stream()
            .max(Comparator.comparing(Candidate::at))
            .map(Candidate::details)
            .orElseGet(() -> new UpdateDetails(null, null, null, null));
  }

  private GitHubRepoResponse getRepository(String owner, String repo) {
    return retryRegistry.retry("github-getRepository").executeSupplier(() -> api.getRepository(owner, repo));
  }

  private List<GitHubItemResponse> getPulls(String owner, String repo) {
    return retryRegistry.retry("github-getPulls").executeSupplier(() -> api.getPulls(owner, repo));
  }

  private List<GitHubItemResponse> getIssues(String owner, String repo) {
    return retryRegistry.retry("github-getIssues").executeSupplier(() -> api.getIssues(owner, repo));
  }

  private static Candidate itemCandidate(GitHubItemResponse item) {
    String author = item.user() == null ? null : item.user().login();
    UpdateDetails details =
            new UpdateDetails(item.title(), author, item.createdAt(), TextPreview.preview(item.body()));
    return new Candidate(item.updatedAt(), details);
  }

  private static Candidate repoCandidate(GitHubRepoResponse repo) {
    String author = repo.owner() == null ? null : repo.owner().login();
    UpdateDetails details =
            new UpdateDetails(
                    repo.fullName(), author, repo.pushedAt(), TextPreview.preview(repo.description()));
    return new Candidate(repo.pushedAt(), details);
  }

  private record Candidate(Instant at, UpdateDetails details) {}
}