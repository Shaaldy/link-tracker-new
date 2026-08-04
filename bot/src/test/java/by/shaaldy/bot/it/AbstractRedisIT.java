package by.shaaldy.bot.it;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {"app.cache.enabled=true", "app.message-transport=HTTP"})
public abstract class AbstractRedisIT {

  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

  static {
    REDIS.start();
  }

  @DynamicPropertySource
  static void redisProps(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired CacheManager cacheManager;

  @BeforeEach
  void clearCaches() {
    cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
  }
}
