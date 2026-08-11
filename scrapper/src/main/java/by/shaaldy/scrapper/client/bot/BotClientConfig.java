package by.shaaldy.scrapper.client.bot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import by.shaaldy.scrapper.client.HttpClientFactorySupport;
import by.shaaldy.scrapper.config.AppProperties;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class BotClientConfig {

  private final AppProperties properties;

  @Bean
  public BotClient botClient() {
    RestClient restClient =
        RestClient.builder()
            .baseUrl(properties.botBaseUrl())
            .requestFactory(HttpClientFactorySupport.build(properties.httpClient().timeout()))
            .build();
    RestClientAdapter adapter = RestClientAdapter.create(restClient);
    return HttpServiceProxyFactory.builderFor(adapter).build().createClient(BotClient.class);
  }
}
