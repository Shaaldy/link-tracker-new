package by.shaaldy.scrapper.client.github;

import java.net.URI;
import java.time.Instant;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import by.shaaldy.scrapper.client.UpdateChecker;
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
}
