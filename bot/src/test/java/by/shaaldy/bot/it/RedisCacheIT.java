package by.shaaldy.bot.it;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import by.shaaldy.bot.client.ScrapperClient;
import by.shaaldy.bot.dto.scrapper.AddLinkRequest;
import by.shaaldy.bot.dto.scrapper.ListLinksResponse;
import by.shaaldy.bot.service.cache.LinkQueryService;

class RedisCacheIT extends AbstractRedisIT {
  private static final long CHAT = 9001L;

  @MockitoBean ScrapperClient scrapperClient;
  @Autowired LinkQueryService linkQueryService;

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
