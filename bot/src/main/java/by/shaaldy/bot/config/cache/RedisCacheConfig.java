package by.shaaldy.bot.config.cache;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import by.shaaldy.bot.config.AppProperties;

/**
 * Конфигурация Spring Cache abstraction для кэшей /list и регистрации чата. Ключи: linksByChat::*,
 * chatRegistered::*. Не относится к буферу дайджеста (см. DigestBuffer, ключи digest:*) — тот
 * использует Redis напрямую, минуя эту конфигурацию.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true")
public class RedisCacheConfig {

  public static final String LINKS_CACHE = "linksByChat";
  public static final String REGISTRATION_CACHE = "chatRegistered";

  @Bean
  public RedisCacheManager cacheManager(
      RedisConnectionFactory connectionFactory, AppProperties properties) {

    RedisCacheConfiguration listConfig = baseConfig().entryTtl(properties.cache().listTtl());

    RedisCacheConfiguration registrationConfig = baseConfig().entryTtl(Duration.ofDays(7));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(listConfig)
        .withCacheConfiguration(REGISTRATION_CACHE, registrationConfig)
        .build();
  }

  private RedisCacheConfiguration baseConfig() {
    return RedisCacheConfiguration.defaultCacheConfig()
        .disableCachingNullValues()
        .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(
            SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
  }
}
