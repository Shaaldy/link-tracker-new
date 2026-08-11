package by.shaaldy.scrapper.client;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;

import by.shaaldy.scrapper.config.AppProperties;

public final class HttpClientFactorySupport {

  private HttpClientFactorySupport() {}

  public static ClientHttpRequestFactory build(AppProperties.HttpClient.Timeout timeout) {
    ClientHttpRequestFactorySettings settings =
        ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(timeout.connect())
            .withReadTimeout(timeout.read());
    return ClientHttpRequestFactoryBuilder.detect().build(settings);
  }
}
