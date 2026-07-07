package by.shaaldy.scrapper.client.github;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface GitHubApi {

  @GetExchange("/repos/{owner}/{repo}")
  GitHubRepoResponse getRepository(@PathVariable String owner, @PathVariable String repo);
}
