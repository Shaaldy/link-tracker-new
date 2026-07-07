package by.shaaldy.scrapper.repository;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import by.shaaldy.scrapper.domain.Link;
import by.shaaldy.scrapper.domain.TrackedLink;

@Repository
public class InMemorySubscriptionRepository implements SubscriptionRepository {

  private final AtomicLong linkIdSeq = new AtomicLong();
  private final Set<Long> chats = ConcurrentHashMap.newKeySet();
  private final Map<URI, Link> linksByUrl = new ConcurrentHashMap<>();
  private final Map<Long, Link> linksById = new ConcurrentHashMap<>();
  private final Map<Long, Map<Long, Subscription>> subsByChat = new ConcurrentHashMap<>();
  private final Map<Long, Set<Long>> subscribersByLink = new ConcurrentHashMap<>();
  private final Map<URI, Instant> checkedAtByUrl = new ConcurrentHashMap<>();

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
    Link link = linksById.remove(linkId);
    if (link != null) {
      linksByUrl.remove(link.getUrl());
      checkedAtByUrl.remove(link.getUrl()); // ← синхронная чистка времени
    }
  }

  @Override
  public boolean chatExists(long chatId) {
    return chats.contains(chatId);
  }

  /* ------------------------- links ------------------------- */

  @Override
  public TrackedLink addLink(long chatId, URI url, List<String> tags, List<String> filters) {
    Link link =
        linksByUrl.computeIfAbsent(
            url,
            u -> {
              Link created = new Link(linkIdSeq.incrementAndGet(), u, Instant.now());
              linksById.put(created.getId(), created);
              checkedAtByUrl.put(u, Instant.EPOCH); // ← первый тик всегда сработает
              return created;
            });

    Subscription sub = new Subscription(new ArrayList<>(tags), new ArrayList<>(filters));
    subsByChat.get(chatId).put(link.getId(), sub);
    subscribersByLink.computeIfAbsent(link.getId(), k -> ConcurrentHashMap.newKeySet()).add(chatId);

    return toDomain(link, sub);
  }

  @Override
  public boolean removeLink(long chatId, URI url) {
    Link link = linksByUrl.get(url);
    if (link == null) {
      return false;
    }
    Map<Long, Subscription> subs = subsByChat.get(chatId);
    if (subs == null || subs.remove(link.getId()) == null) {
      return false;
    }
    Set<Long> subscribers = subscribersByLink.get(link.getId());
    if (subscribers != null) {
      subscribers.remove(chatId);
      if (subscribers.isEmpty()) {
        subscribersByLink.remove(link.getId());
        linksById.remove(link.getId());
        linksByUrl.remove(url);
        checkedAtByUrl.remove(url); // ← синхронная чистка времени
      }
    }
    return true;
  }

  @Override
  public boolean subscriptionExists(long chatId, URI url) {
    Link link = linksByUrl.get(url);
    if (link == null) {
      return false;
    }
    Map<Long, Subscription> subs = subsByChat.get(chatId);
    return subs != null && subs.containsKey(link.getId());
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
          Link link = linksById.get(linkId);
          if (link != null) {
            result.add(toDomain(link, sub));
          }
        });
    return result;
  }

  @Override
  public Set<Long> findSubscribers(URI url) {
    Link link = linksByUrl.get(url);
    if (link == null) {
      return Set.of();
    }
    Set<Long> subscribers = subscribersByLink.get(link.getId());
    return subscribers == null ? Set.of() : Set.copyOf(subscribers);
  }

  @Override
  public Instant getCheckedAt(URI url) {
    return checkedAtByUrl.get(url);
  }

  @Override
  public void updateCheckedAt(URI url, Instant checkedAt) {
    checkedAtByUrl.put(url, checkedAt);
  }

  @Override
  public Collection<Link> findAllLinks() {
    return List.copyOf(linksByUrl.values());
  }

  /* ------------------------- helpers ------------------------- */

  private TrackedLink toDomain(Link link, Subscription sub) {
    return new TrackedLink(link.getId(), link.getUrl(), sub.tags(), sub.filters());
  }

  private record Subscription(List<String> tags, List<String> filters) {}
}
