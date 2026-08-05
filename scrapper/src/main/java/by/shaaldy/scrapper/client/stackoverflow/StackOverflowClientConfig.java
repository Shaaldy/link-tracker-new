package by.shaaldy.scrapper.client.stackoverflow;

import by.shaaldy.scrapper.client.HttpClientFactorySupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import by.shaaldy.scrapper.config.AppProperties;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class StackOverflowClientConfig {

  private final AppProperties properties;

  @Bean
  StackOverflowApi stackOverflowApi() {
    RestClient restClient =
        RestClient.builder().baseUrl(properties.stackoverflow().baseUrl())
                .requestFactory(HttpClientFactorySupport.build(properties.httpClient().timeout()))
                .build();
    HttpServiceProxyFactory factory =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
    return factory.createClient(StackOverflowApi.class);
  }
}
