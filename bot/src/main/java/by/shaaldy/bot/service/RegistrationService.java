package by.shaaldy.bot.service.cache;


import by.shaaldy.bot.config.cache.RedisCacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import by.shaaldy.bot.client.ScrapperClient;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RegistrationService {

  private final ScrapperClient scrapperClient;

  @Cacheable(cacheNames = RedisCacheConfig.REGISTRATION_CACHE, key = "#chatId", unless = "!#result")
  public boolean isRegistered(long chatId) {
    return Boolean.TRUE.equals(scrapperClient.existChat(chatId));
  }

  @CacheEvict(cacheNames = RedisCacheConfig.REGISTRATION_CACHE, key = "#chatId")
  public void markUnregistered(long chatId) {}
}
