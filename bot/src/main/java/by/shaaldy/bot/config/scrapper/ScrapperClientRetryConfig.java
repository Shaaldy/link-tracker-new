package by.shaaldy.bot.config.scrapper;

import by.shaaldy.bot.client.ScrapperClient;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ScrapperClientRetryConfig {

    @Bean
    @Primary
    public ScrapperClient resilientScrapperClient(ScrapperClient delegate, RetryRegistry retryRegistry) {
        InvocationHandler handler = new RetryingInvocationHandler(delegate, retryRegistry);
        return (ScrapperClient)
                Proxy.newProxyInstance(
                        ScrapperClient.class.getClassLoader(), new Class<?>[] {ScrapperClient.class}, handler);
    }

    private record RetryingInvocationHandler(ScrapperClient delegate, RetryRegistry retryRegistry)
            implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
            Retry retry = retryRegistry.retry("scrapper-" + method.getName());
            try {
                return retry.executeCheckedSupplier(() -> method.invoke(delegate, args));
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}