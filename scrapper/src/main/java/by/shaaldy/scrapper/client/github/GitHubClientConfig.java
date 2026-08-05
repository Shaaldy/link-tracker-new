package by.shaaldy.scrapper.client.github;

import by.shaaldy.scrapper.client.HttpClientFactorySupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import by.shaaldy.scrapper.config.AppProperties;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class GitHubClientConfig {

  private final AppProperties properties;

  @Bean
  GitHubApi gitHubApi() {
    AppProperties.GitHub github = properties.github();
    RestClient.Builder builder =
        RestClient.builder()
            .baseUrl(github.baseUrl()).requestFactory(HttpClientFactorySupport.build(properties.httpClient().timeout()))
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28");
    if (StringUtils.hasText(github.token())) {
      builder.defaultHeader("Authorization", "Bearer " + github.token());
    }
    HttpServiceProxyFactory factory =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build())).build();
    return factory.createClient(GitHubApi.class);
  }
}
