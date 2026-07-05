package by.shaaldy.scrapper.repository;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import by.shaaldy.scrapper.domain.TrackedLink;

@Repository
public class InMemorySubscriptionRepository implements SubscriptionRepository {

  /** Генератор суррогатных id ссылок (аналог BIGSERIAL). */
  private final AtomicLong linkIdSeq = new AtomicLong();

  /** Аналог таблицы chats: множество зарегистрированных чатов. */
  private final Set<Long> chats = ConcurrentHashMap.newKeySet();

  /** Аналог таблицы links: url -> ссылка (глобально уникальна по url). */
  private final Map<URI, StoredLink> linksByUrl = new ConcurrentHashMap<>();

  /** Обратный индекс по id ссылки (для Stage 3: checked_at, обход). */
  private final Map<Long, StoredLink> linksById = new ConcurrentHashMap<>();

  /** Аналог chat_links: chatId -> (linkId -> подписка с tags/filters). */
  private final Map<Long, Map<Long, Subscription>> subsByChat = new ConcurrentHashMap<>();

  /** Обратный индекс: linkId -> подписчики (для Stage 3 планировщика). */
  private final Map<Long, Set<Long>> subscribersByLink = new ConcurrentHashMap<>();

  /* ------------------------- chats ------------------------- */

  @Override
  public boolean registerChat(long chatId) {
    boolean added = chats.add(chatId);
    if (added) {
      subsByChat.put(chatId, new ConcurrentHashMap<>());
    }
    return added;
  }

  @Override
  public boolean removeChat(long chatId) {
    if (!chats.remove(chatId)) {
      return false;
    }
    Map<Long, Subscription> subs = subsByChat.remove(chatId);
    subs.keySet()
        .forEach(
            linkId -> {
              Set<Long> subscribers = subscribersByLink.get(linkId);
              subscribers.remove(chatId);
              if (subscribers.isEmpty()) {
                removeOrphanLink(linkId);
              }
            });
    return true;
  }

  private void removeOrphanLink(long linkId) {
    subscribersByLink.remove(linkId);
    StoredLink link = linksById.remove(linkId);
    if (link != null) {
      linksByUrl.remove(link.url());
    }
  }

  @Override
  public boolean chatExists(long chatId) {
    return chats.contains(chatId);
  }

  /* ------------------------- links ------------------------- */

  @Override
  public TrackedLink addLink(long chatId, URI url, List<String> tags, List<String> filters) {
    // ссылка глобально уникальна: переиспользуем существующую или заводим новую
    StoredLink link =
        linksByUrl.computeIfAbsent(
            url,
            u -> {
              StoredLink created = new StoredLink(linkIdSeq.incrementAndGet(), u, Instant.now());
              linksById.put(created.id(), created);
              return created;
            });

    // подписка чата с его собственными tags/filters
    Subscription sub = new Subscription(new ArrayList<>(tags), new ArrayList<>(filters));
    subsByChat.get(chatId).put(link.id(), sub);
    subscribersByLink.computeIfAbsent(link.id(), k -> ConcurrentHashMap.newKeySet()).add(chatId);

    return toDomain(link, sub);
  }

  @Override
  public boolean removeLink(long chatId, URI url) {
    StoredLink link = linksByUrl.get(url);
    if (link == null) {
      return false;
    }
    Map<Long, Subscription> subs = subsByChat.get(chatId);
    if (subs == null || subs.remove(link.id()) == null) {
      return false; // чат не был подписан на эту ссылку
    }
    Set<Long> subscribers = subscribersByLink.get(link.id());
    if (subscribers != null) {
      subscribers.remove(chatId);
      if (subscribers.isEmpty()) {
        subscribersByLink.remove(link.id());
        linksById.remove(link.id());
        linksByUrl.remove(url);
      }
    }
    return true;
  }

  @Override
  public boolean subscriptionExists(long chatId, URI url) {
    StoredLink link = linksByUrl.get(url);
    if (link == null) {
      return false;
    }
    Map<Long, Subscription> subs = subsByChat.get(chatId);
    return subs != null && subs.containsKey(link.id());
  }

  @Override
  public List<TrackedLink> findLinksByChat(long chatId) {
    Map<Long, Subscription> subs = subsByChat.get(chatId);
    if (subs == null || subs.isEmpty()) {
      return List.of();
    }
    List<TrackedLink> result = new ArrayList<>(subs.size());
    subs.forEach(
        (linkId, sub) -> {
          StoredLink link = linksById.get(linkId);
          if (link != null) {
            result.add(toDomain(link, sub));
          }
        });
    return result;
  }

  @Override
  public Set<Long> findSubscribers(URI url) {
    StoredLink link = linksByUrl.get(url);
    if (link == null) {
      return Set.of();
    }
    Set<Long> subscribers = subscribersByLink.get(link.id());
    return subscribers == null ? Set.of() : Set.copyOf(subscribers);
  }

  @Override
  public Collection<URI> findAllUrls() {
    return Set.copyOf(linksByUrl.keySet());
  }

  /* ------------------------- helpers ------------------------- */

  private TrackedLink toDomain(StoredLink link, Subscription sub) {
    return new TrackedLink(link.id(), link.url(), sub.tags(), sub.filters());
  }

  /** Внутренний holder ссылки (аналог строки таблицы links). */
  private record StoredLink(long id, URI url, Instant createdAt) {}

  /** Внутренний holder подписки (аналог строки chat_links + её tags/filters). */
  private record Subscription(List<String> tags, List<String> filters) {}
}
