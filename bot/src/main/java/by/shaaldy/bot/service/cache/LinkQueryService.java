package by.shaaldy.bot.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import by.shaaldy.bot.client.ScrapperClient;
import by.shaaldy.bot.config.cache.RedisCacheConfig;
import by.shaaldy.bot.dto.scrapper.AddLinkRequest;
import by.shaaldy.bot.dto.scrapper.LinkResponse;
import by.shaaldy.bot.dto.scrapper.ListLinksResponse;
import by.shaaldy.bot.dto.scrapper.RemoveLinkRequest;
import by.shaaldy.bot.dto.scrapper.TagRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LinkQueryService {

  private final ScrapperClient scrapperClient;

  @Cacheable(cacheNames = RedisCacheConfig.LINKS_CACHE, key = "#chatId")
  public ListLinksResponse listLinks(long chatId) {
    return scrapperClient.listLinks(chatId);
  }

  // тег-запрос мимо кэша — осознанно (см. пояснение)
  public ListLinksResponse listLinksByTag(long chatId, String tag) {
    return scrapperClient.listLinksByTag(chatId, tag);
  }

  @CacheEvict(cacheNames = RedisCacheConfig.LINKS_CACHE, key = "#chatId")
  public LinkResponse addLink(long chatId, AddLinkRequest request) {
    return scrapperClient.addLink(chatId, request);
  }

  @CacheEvict(cacheNames = RedisCacheConfig.LINKS_CACHE, key = "#chatId")
  public LinkResponse removeLink(long chatId, RemoveLinkRequest request) {
    return scrapperClient.removeLink(chatId, request);
  }

  @CacheEvict(cacheNames = RedisCacheConfig.LINKS_CACHE, key = "#chatId")
  public LinkResponse addTag(long chatId, TagRequest request) {
    return scrapperClient.addTag(chatId, request);
  }

  @CacheEvict(cacheNames = RedisCacheConfig.LINKS_CACHE, key = "#chatId")
  public LinkResponse removeTag(long chatId, TagRequest request) {
    return scrapperClient.removeTag(chatId, request);
  }
}
