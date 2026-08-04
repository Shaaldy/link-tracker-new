package by.shaaldy.bot.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import by.shaaldy.bot.client.ScrapperClient;
import by.shaaldy.bot.config.cache.RedisCacheConfig;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RegistrationService {

  private final ScrapperClient scrapperClient;

  @Cacheable(cacheNames = RedisCacheConfig.REGISTRATION_CACHE, key = "#chatId", unless = "!#result")
  public boolean isRegistered(long chatId) {
    return Boolean.TRUE.equals(scrapperClient.existChat(chatId));
  }

  @CachePut(cacheNames = RedisCacheConfig.REGISTRATION_CACHE, key = "#chatId")
  public boolean registerIfAbsent(long chatId) {
    if (isRegistered(chatId)) {
      return false;
    }
    scrapperClient.registerChat(chatId);
    return true;
  }

  @CacheEvict(cacheNames = RedisCacheConfig.REGISTRATION_CACHE, key = "#chatId")
  public void unregister(long chatId) {
    scrapperClient.deleteChat(chatId);
  }
}
