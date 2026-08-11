package by.shaaldy.scrapper.notification;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import by.shaaldy.scrapper.client.bot.BotClient;
import by.shaaldy.scrapper.config.AppProperties;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

@ExtendWith(MockitoExtension.class)
class NotificationFallbackE2ETest {

  private static WireMockServer wireMock;
  private static BotClient botClient;

  @Mock private KafkaTemplate<String, LinkUpdate> kafkaTemplate;

  @BeforeAll
  static void startWireMock() {
    wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    wireMock.start();

    ClientHttpRequestFactorySettings settings =
        ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(Duration.ofSeconds(1))
            .withReadTimeout(Duration.ofMillis(300));
    RestClient restClient =
        RestClient.builder()
            .baseUrl(wireMock.baseUrl())
            .requestFactory(ClientHttpRequestFactoryBuilder.simple().build(settings))
            .build();
    botClient =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(BotClient.class);
  }

  @AfterAll
  static void stopWireMock() {
    wireMock.stop();
  }

  @AfterEach
  void resetStubs() {
    wireMock.resetAll();
  }

  private HttpNotificationSender newHttpSender() {
    RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom().maxAttempts(1).build());
    CircuitBreakerRegistry circuitBreakerRegistry =
        CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                .slidingWindowSize(1)
                .minimumNumberOfCalls(1)
                .failureRateThreshold(100)
                .permittedNumberOfCallsInHalfOpenState(1)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .build());
    return new HttpNotificationSender(botClient, retryRegistry, circuitBreakerRegistry);
  }

  private AppProperties propertiesWithTransport(AppProperties.MessageTransport transport) {
    AppProperties.Kafka.Topics topics =
        new AppProperties.Kafka.Topics("link-updates", "link-updates-dlq");
    AppProperties.Kafka kafka = new AppProperties.Kafka(topics);
    AppProperties.Notification notification = new AppProperties.Notification(Duration.ofSeconds(1));
    return new AppProperties(
        null, null, null, null, null, transport, kafka, null, null, null, null, notification);
  }

  @Test
  void httpDown_kafkaHealthy_fallsBackToKafka() {
    wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(503)));

    AppProperties properties = propertiesWithTransport(AppProperties.MessageTransport.HTTP);
    KafkaNotificationSender kafkaSender = new KafkaNotificationSender(kafkaTemplate, properties);
    FallbackNotificationSender sender =
        new FallbackNotificationSender(newHttpSender(), kafkaSender, properties);

    LinkUpdate update = new LinkUpdate().id(1L);
    org.mockito.Mockito.when(kafkaTemplate.send("link-updates", "1", update))
        .thenReturn(CompletableFuture.completedFuture(null));

    sender.send(update);

    verify(kafkaTemplate).send("link-updates", "1", update);
    wireMock.verify(1, postRequestedFor(anyUrl()));
  }

  @Test
  void kafkaDown_httpHealthy_fallsBackToHttp() {
    wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));

    AppProperties properties = propertiesWithTransport(AppProperties.MessageTransport.KAFKA);
    KafkaNotificationSender kafkaSender = new KafkaNotificationSender(kafkaTemplate, properties);
    FallbackNotificationSender sender =
        new FallbackNotificationSender(newHttpSender(), kafkaSender, properties);

    LinkUpdate update = new LinkUpdate().id(1L);
    CompletableFuture<Object> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("kafka broker unreachable"));
    org.mockito.Mockito.when(kafkaTemplate.send("link-updates", "1", update))
        .thenReturn((CompletableFuture) failedFuture);

    sender.send(update);

    wireMock.verify(1, postRequestedFor(anyUrl()));
  }
}
