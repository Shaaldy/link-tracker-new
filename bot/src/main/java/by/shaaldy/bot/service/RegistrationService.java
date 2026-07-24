package by.shaaldy.bot.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import by.shaaldy.bot.client.ScrapperClient;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RegistrationService {

  private final ScrapperClient scrapperClient;

  private final Set<Long> registered = ConcurrentHashMap.newKeySet();

  public boolean isRegistered(long chatId) {
    if (registered.contains(chatId)) {
      return true;
    }
    boolean existsInDb = scrapperClient.existChat(chatId);
    if (existsInDb) {
      registered.add(chatId);
    }
    return existsInDb;
  }

  public void markRegistered(long chatId) {
    registered.add(chatId);
  }

  public void markUnregistered(long chatId) {
    registered.remove(chatId);
  }
}
