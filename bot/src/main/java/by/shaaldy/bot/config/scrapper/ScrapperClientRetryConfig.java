package by.shaaldy.bot.config.scrapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import by.shaaldy.bot.client.ScrapperClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;

@Configuration
public class ScrapperClientRetryConfig {

  @Bean
  @Primary
  public ScrapperClient resilientScrapperClient(
      ScrapperClient delegate,
      RetryRegistry retryRegistry,
      CircuitBreakerRegistry circuitBreakerRegistry) {
    InvocationHandler handler =
        new RetryingInvocationHandler(delegate, retryRegistry, circuitBreakerRegistry);
    return (ScrapperClient)
        Proxy.newProxyInstance(
            ScrapperClient.class.getClassLoader(), new Class<?>[] {ScrapperClient.class}, handler);
  }

  private record RetryingInvocationHandler(
      ScrapperClient delegate,
      RetryRegistry retryRegistry,
      CircuitBreakerRegistry circuitBreakerRegistry)
      implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args)
        throws Throwable {
      String name = "scrapper-" + method.getName();
      try {
        return circuitBreakerRegistry
            .circuitBreaker(name)
            .executeCheckedSupplier(
                () ->
                    retryRegistry
                        .retry(name)
                        .executeCheckedSupplier(() -> method.invoke(delegate, args)));
      } catch (InvocationTargetException e) {
        throw e.getCause();
      }
    }
  }
}
