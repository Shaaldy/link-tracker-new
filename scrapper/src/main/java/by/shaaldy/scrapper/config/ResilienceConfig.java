package by.shaaldy.scrapper.config;

import java.util.function.Predicate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientResponseException;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

@Configuration
public class ResilienceConfig {

  @Bean
  public RetryRegistry retryRegistry(AppProperties properties) {
    RetryConfig config =
        RetryConfig.custom()
            .maxAttempts(properties.retry().maxAttempts())
            .waitDuration(properties.retry().waitDuration())
            .retryOnException(retryableStatusPredicate(properties.retry()))
            .build();
    return RetryRegistry.of(config);
  }

  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry(AppProperties properties) {
    AppProperties.CircuitBreaker cb = properties.circuitBreaker();
    CircuitBreakerConfig config =
        CircuitBreakerConfig.custom()
            .slidingWindowSize(cb.slidingWindow())
            .minimumNumberOfCalls(cb.minimumNumberOfCalls())
            .failureRateThreshold(cb.failureRateThreshold())
            .permittedNumberOfCallsInHalfOpenState(cb.permittedCallsInHalfOpenState())
            .waitDurationInOpenState(cb.waitDurationOpenState())
            .build();
    return CircuitBreakerRegistry.of(config);
  }

  private static Predicate<Throwable> retryableStatusPredicate(AppProperties.Retry retry) {
    return throwable -> {
      if (!(throwable instanceof RestClientResponseException e)) {
        return false;
      }
      HttpStatusCode status = e.getStatusCode();
      return retry.retryableStatusCodes().contains(status.value());
    };
  }
}
