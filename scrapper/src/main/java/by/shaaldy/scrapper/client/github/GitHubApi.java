package by.shaaldy.scrapper.client.github;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface GitHubApi {

  @GetExchange("/repos/{owner}/{repo}")
  GitHubRepoResponse getRepository(@PathVariable String owner, @PathVariable String repo);

  /** Последние PR (sort=updated, desc) — для детализации. */
  @GetExchange("/repos/{owner}/{repo}/pulls?state=all&sort=updated&direction=desc&per_page=1")
  List<GitHubItemResponse> getPulls(@PathVariable String owner, @PathVariable String repo);

  /** Последние Issues (sort=updated, desc) — для детализации. */
  @GetExchange("/repos/{owner}/{repo}/issues?state=all&sort=updated&direction=desc&per_page=1")
  List<GitHubItemResponse> getIssues(@PathVariable String owner, @PathVariable String repo);
}
