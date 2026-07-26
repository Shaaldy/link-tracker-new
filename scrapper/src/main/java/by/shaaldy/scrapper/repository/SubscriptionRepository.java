package by.shaaldy.scrapper.repository;

import java.net.URI;
import java.util.List;
import java.util.Set;

import by.shaaldy.scrapper.domain.TrackedLink;

public interface SubscriptionRepository {
  boolean registerChat(long chatId);

  boolean removeChat(long chatId);

  boolean chatExists(long chatId);

  TrackedLink addLink(long chatId, URI url, List<String> tags, List<String> filters);

  boolean removeLink(long chatId, URI url);

  boolean subscriptionExists(long chatId, URI url);

  List<TrackedLink> findLinksByChat(long chatId);

  Set<Long> findSubscribers(URI url);

  void deleteAll();

  List<TrackedLink> findLinksByChatAndTag(long chatId, String tag);

  Set<String> findTagsByChat(long chatId);

  boolean addTag(long chatId, URI url, String tag);

  boolean removeTag(long chatId, URI url, String tag);
}
