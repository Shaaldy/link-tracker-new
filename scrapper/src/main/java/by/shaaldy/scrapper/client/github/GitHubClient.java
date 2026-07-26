package by.shaaldy.scrapper.client.github;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

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

    // Три кандидата: последний PR, последний Issue, репо-событие (общий случай — пуш в код и т.п.).
    // Выбираем самое свежее по времени. Репо-кандидат есть всегда; PR/Issue могут отсутствовать.
    List<Candidate> candidates = new ArrayList<>();

    GitHubRepoResponse repository = api.getRepository(owner, repo);
    candidates.add(repoCandidate(repository));

    api.getPulls(owner, repo).stream()
        .findFirst()
        .map(GitHubClient::itemCandidate)
        .ifPresent(candidates::add);
    api.getIssues(owner, repo).stream()
        .findFirst()
        .map(GitHubClient::itemCandidate)
        .ifPresent(candidates::add);

    return candidates.stream()
        .max(Comparator.comparing(Candidate::at))
        .map(Candidate::details)
        .orElseGet(() -> new UpdateDetails(null, null, null, null));
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

  /** Кандидат детализации с временем события — для выбора самого свежего. */
  private record Candidate(Instant at, UpdateDetails details) {}
}
