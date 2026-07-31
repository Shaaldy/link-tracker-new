package by.shaaldy.bot.it;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import by.shaaldy.bot.client.ScrapperClient;
import by.shaaldy.bot.dto.scrapper.AddLinkRequest;
import by.shaaldy.bot.dto.scrapper.ListLinksResponse;
import by.shaaldy.bot.service.LinkQueryService;

@Testcontainers
@SpringBootTest(properties = {"app.cache.enabled=true", "app.message-transport=HTTP"})
class RedisCacheIT {
  private static final long CHAT = 9001L;

  @MockitoBean ScrapperClient scrapperClient;
  @Autowired LinkQueryService linkQueryService;
  @Autowired CacheManager cacheManager;

  @BeforeEach
  void clearCaches() {
    cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
  }

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProps(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Test
  void listLinks_secondCall_servedFromCache() {
    when(scrapperClient.listLinks(CHAT)).thenReturn(new ListLinksResponse().size(0));

    linkQueryService.listLinks(CHAT);
    linkQueryService.listLinks(CHAT);

    verify(scrapperClient, times(1)).listLinks(CHAT); // второй раз — из кэша
  }

  @Test
  void addLink_evictsCache_nextListHitsClient() {
    when(scrapperClient.listLinks(CHAT)).thenReturn(new ListLinksResponse().size(0));

    linkQueryService.listLinks(CHAT); // populate
    linkQueryService.addLink(CHAT, new AddLinkRequest()); // evict
    linkQueryService.listLinks(CHAT); // снова к клиенту

    verify(scrapperClient, times(2)).listLinks(CHAT);
  }
}
