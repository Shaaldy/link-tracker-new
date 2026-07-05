package by.shaaldy.scrapper.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import by.shaaldy.scrapper.config.AppProperties;

@Configuration
public class BotClientConfig {

  @Bean
  public BotClient botClient(AppProperties properties) {
    RestClient restClient = RestClient.builder().baseUrl(properties.botBaseUrl()).build();
    RestClientAdapter adapter = RestClientAdapter.create(restClient);
    return HttpServiceProxyFactory.builderFor(adapter).build().createClient(BotClient.class);
  }
}
