package by.shaaldy.bot.config.scrapper;

import java.io.IOException;

import by.shaaldy.bot.client.HttpClientFactorySupport;
import by.shaaldy.bot.client.ScrapperClient;
import by.shaaldy.bot.config.AppProperties;
import by.shaaldy.bot.exception.ScrapperApiException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import by.shaaldy.bot.dto.scrapper.ApiErrorResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ScrapperClientConfig {

  private final AppProperties properties;

  @Bean
  public ScrapperClient scrapperClientDelegate(ObjectMapper objectMapper) {
    RestClient restClient =
            RestClient.builder()
                    .baseUrl(properties.scrapperBaseUrl())
                    .requestFactory(HttpClientFactorySupport.build(properties.httpClient().timeout()))
                    .defaultStatusHandler(
                            HttpStatusCode::isError,
                            (request, response) -> {
                              ApiErrorResponse body =
                                      readError(objectMapper, response.getBody(), response.getStatusCode());
                              throw new ScrapperApiException(response.getStatusCode(), body);
                            })
                    .build();

    RestClientAdapter adapter = RestClientAdapter.create(restClient);
    return HttpServiceProxyFactory.builderFor(adapter).build().createClient(ScrapperClient.class);
  }

  private static ApiErrorResponse readError(
          ObjectMapper mapper, java.io.InputStream body, HttpStatusCode status) {
    try {
      return mapper.readValue(body, ApiErrorResponse.class);
    } catch (IOException e) {
      return new ApiErrorResponse().description("Ошибка сервиса (" + status + ")");
    }
  }
}
