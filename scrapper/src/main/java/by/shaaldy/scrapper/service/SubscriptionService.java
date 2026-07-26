package by.shaaldy.scrapper.service;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import by.shaaldy.scrapper.domain.TrackedLink;
import by.shaaldy.scrapper.exception.ChatAlreadyExistsException;
import by.shaaldy.scrapper.exception.ChatNotFoundException;
import by.shaaldy.scrapper.exception.LinkAlreadyTrackedException;
import by.shaaldy.scrapper.exception.LinkNotFoundException;
import by.shaaldy.scrapper.repository.SubscriptionRepository;
import by.shaaldy.scrapper.validation.LinkValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {
  private final SubscriptionRepository repository;
  private final LinkValidator linkValidator;

  /* --- chats --- */

  public void registerChat(long chatId) {
    if (!repository.registerChat(chatId)) {
      log.debug("Повторная регистрация чата {}", chatId);
      throw new ChatAlreadyExistsException(chatId);
    }
    log.info("Чат {} зарегистрирован", chatId);
  }

  public void removeChat(long chatId) {
    if (!repository.removeChat(chatId)) {
      log.debug("Удаление несуществующего чата {}", chatId);
      throw new ChatNotFoundException(chatId);
    }
    log.info("Чат {} удалён", chatId);
  }

  public boolean existChat(long chatId) {
    return repository.chatExists(chatId);
  }

  /* --- links --- */

  public TrackedLink addLink(long chatId, URI url, List<String> tags, List<String> filters) {
    linkValidator.validate(url);
    requireChat(chatId);
    if (repository.subscriptionExists(chatId, url)) {
      log.debug("Чат {} уже отслеживает {}", chatId, url);
      throw new LinkAlreadyTrackedException(chatId, url);
    }
    TrackedLink added = repository.addLink(chatId, url, normalize(tags), normalize(filters));
    log.info("Чат {} начал отслеживать {}", chatId, url);
    return added;
  }

  public TrackedLink removeLink(long chatId, URI url) {
    requireChat(chatId);
    linkValidator.validate(url);
    TrackedLink removed =
        repository.findLinksByChat(chatId).stream()
            .filter(link -> link.url().equals(url))
            .findFirst()
            .orElseThrow(
                () -> {
                  log.debug("Чат {} не отслеживает {}", chatId, url);
                  return new LinkNotFoundException(chatId, url);
                });
    repository.removeLink(chatId, url);
    log.info("Чат {} перестал отслеживать {}", chatId, url);
    return removed;
  }

  public List<TrackedLink> getLinks(long chatId) {
    requireChat(chatId);
    List<TrackedLink> links = repository.findLinksByChat(chatId);
    log.debug("Чат {} запросил список ссылок, найдено {}", chatId, links.size());
    return links;
  }

  /* --- tags --- */

  public List<TrackedLink> getLinksByTag(long chatId, String tag) {
    requireChat(chatId);
    if (isBlank(tag)) {
      return List.of();
    }
    List<TrackedLink> links = repository.findLinksByChatAndTag(chatId, tag.strip());
    log.debug("Чат {} запросил ссылки по тегу '{}', найдено {}", chatId, tag, links.size());
    return links;
  }

  public Set<String> getTags(long chatId) {
    requireChat(chatId);
    Set<String> tags = repository.findTagsByChat(chatId);
    log.debug("Чат {} запросил теги, найдено {}", chatId, tags.size());
    return tags;
  }

  public boolean addTag(long chatId, URI url, String tag) {
    requireChat(chatId);
    linkValidator.validate(url);
    if (isBlank(tag)) {
      return false;
    }
    boolean added = repository.addTag(chatId, url, tag.strip());
    if (added) {
      log.info("Чат {} добавил тег '{}' к {}", chatId, tag, url);
    }
    return added;
  }

  public boolean removeTag(long chatId, URI url, String tag) {
    requireChat(chatId);
    linkValidator.validate(url);
    if (isBlank(tag)) {
      return false;
    }
    boolean removed = repository.removeTag(chatId, url, tag.strip());
    if (removed) {
      log.info("Чат {} убрал тег '{}' у {}", chatId, tag, url);
    }
    return removed;
  }

  /* --- helpers --- */

  private void requireChat(long chatId) {
    if (!repository.chatExists(chatId)) {
      log.debug("Обращение к несуществующему чату {}", chatId);
      throw new ChatNotFoundException(chatId);
    }
  }

  private static List<String> normalize(List<String> values) {
    return values == null ? List.of() : values;
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
