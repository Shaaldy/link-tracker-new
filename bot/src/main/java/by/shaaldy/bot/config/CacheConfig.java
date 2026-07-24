package by.shaaldy.bot.config;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {
  @Bean
  Set<Long> registered() {
    return ConcurrentHashMap.newKeySet();
  }
}
