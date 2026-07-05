package by.shaaldy.scrapper.repository;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import by.shaaldy.scrapper.domain.TrackedLink;

public interface SubscriptionRepository {

  /* --- chats --- */
  boolean registerChat(long chatId); // true если создан, false если уже был

  boolean removeChat(long chatId); // true если удалён, false если не было

  boolean chatExists(long chatId);

  /* --- links per chat --- */
  TrackedLink addLink(long chatId, URI url, List<String> tags, List<String> filters);

  boolean removeLink(long chatId, URI url); // true если убрана, false если подписки не было

  boolean subscriptionExists(long chatId, URI url);

  List<TrackedLink> findLinksByChat(long chatId);

  /* --- reverse: для Stage 3 планировщика --- */
  Set<Long> findSubscribers(URI url); // кто подписан на ссылку

  Collection<URI> findAllUrls();
}
