package by.shaaldy.bot.client;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import by.shaaldy.bot.config.AppProperties;
import by.shaaldy.bot.dto.scrapper.ApiErrorResponse;

@Configuration
public class ScrapperClientConfig {

  @Bean
  public ScrapperClient scrapperClient(AppProperties properties, ObjectMapper objectMapper) {
    RestClient restClient =
        RestClient.builder()
            .baseUrl(properties.scrapperBaseUrl())
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
