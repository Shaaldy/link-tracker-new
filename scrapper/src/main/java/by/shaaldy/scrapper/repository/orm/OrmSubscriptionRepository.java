package by.shaaldy.scrapper.repository.orm;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import by.shaaldy.scrapper.domain.TrackedLink;
import by.shaaldy.scrapper.repository.SubscriptionRepository;
import by.shaaldy.scrapper.repository.orm.jpa.ChatJpaRepository;
import by.shaaldy.scrapper.repository.orm.jpa.ChatLinkJpaRepository;
import by.shaaldy.scrapper.repository.orm.jpa.LinkJpaRepository;
import by.shaaldy.scrapper.repository.orm.jpa.entity.ChatEntity;
import by.shaaldy.scrapper.repository.orm.jpa.entity.ChatLinkEntity;
import by.shaaldy.scrapper.repository.orm.jpa.entity.ChatLinkId;
import by.shaaldy.scrapper.repository.orm.jpa.entity.LinkEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA/Hibernate-реализация CRUD подписок. Бин создаётся {@code OrmAccessConfig} при
 * access-type=ORM.
 *
 * <p>Класс транзакционный целиком: конвертация ChatLinkEntity → TrackedLink читает LAZY-коллекции
 * tags/filters, что требует открытой сессии Hibernate. Entity не текут наружу — на границе каждого
 * метода происходит маппинг в доменные типы.
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
public class OrmSubscriptionRepository implements SubscriptionRepository {

  private final ChatJpaRepository chats;
  private final LinkJpaRepository links;
  private final ChatLinkJpaRepository chatLinks;

  /* --- chats --- */

  @Override
  public boolean registerChat(long chatId) {
    if (chats.existsById(chatId)) {
      return false;
    }
    chats.save(new ChatEntity(chatId));
    return true;
  }

  @Override
  public boolean removeChat(long chatId) {
    if (!chats.existsById(chatId)) {
      return false;
    }
    chats.deleteById(chatId); // каскад в схеме снесёт chat_links/link_tags/link_filters
    return true;
  }

  @Override
  public boolean chatExists(long chatId) {
    return chats.existsById(chatId);
  }

  /* --- links --- */

  @Override
  public TrackedLink addLink(long chatId, URI url, List<String> tags, List<String> filters) {
    LinkEntity link = insertOrGetLink(url);
    ChatLinkId id = new ChatLinkId(chatId, link.getId());
    chatLinks.save(new ChatLinkEntity(id, tags, filters));
    return new TrackedLink(link.getId(), url, tags, filters);
  }

  private LinkEntity insertOrGetLink(URI url) {
    return links
        .findByUrl(url.toString())
        .orElseGet(
            () -> {
              LinkEntity e = new LinkEntity();
              e.setUrl(url.toString());
              return links.save(e);
            });
  }

  @Override
  public boolean removeLink(long chatId, URI url) {
    LinkEntity link = links.findByUrl(url.toString()).orElse(null);
    if (link == null) {
      return false;
    }
    ChatLinkId id = new ChatLinkId(chatId, link.getId());
    if (!chatLinks.existsById(id)) {
      return false;
    }
    chatLinks.deleteById(id);
    // осиротевшую ссылку (без подписок) удаляем
    if (chatLinks.findById_LinkId(link.getId()).isEmpty()) {
      links.delete(link);
    }
    return true;
  }

  @Override
  public boolean subscriptionExists(long chatId, URI url) {
    return links
        .findByUrl(url.toString())
        .map(link -> chatLinks.existsById(new ChatLinkId(chatId, link.getId())))
        .orElse(false);
  }

  @Override
  public List<TrackedLink> findLinksByChat(long chatId) {
    // TODO(stage-2): N+1 — toTrackedLink подтягивает LinkEntity по id на каждую подписку.
    // SQL-ветка обходится одним JOIN; здесь тощая entity без @ManyToOne на link провоцирует
    // отдельный запрос за url. Приемлемо для Stage 2 (десятки ссылок на чат); оптимизация —
    // HQL-проекция с join по link_id.
    return chatLinks.findById_ChatId(chatId).stream()
        .map(this::toTrackedLink)
        .collect(Collectors.toList());
  }

  @Override
  public Set<Long> findSubscribers(URI url) {
    return links
        .findByUrl(url.toString())
        .map(
            link ->
                chatLinks.findById_LinkId(link.getId()).stream()
                    .map(cl -> cl.getId().getChatId())
                    .collect(Collectors.toSet()))
        .orElse(Set.of());
  }

  @Override
  public void deleteAll() {
    chatLinks.deleteAll();
    chats.deleteAll();
    links.deleteAll();
  }

  /**
   * Конвертирует подписку в домен. Требует url ссылки, которого нет в ChatLinkEntity — подтягиваем
   * LinkEntity по link_id. Внутри транзакции, LAZY-коллекции доступны.
   */
  private TrackedLink toTrackedLink(ChatLinkEntity cl) {
    long linkId = cl.getId().getLinkId();
    URI url =
        links
            .findById(linkId)
            .map(LinkEntity::getUrl)
            .map(URI::create)
            .orElseThrow(() -> new IllegalStateException("Link " + linkId + " not found"));
    return new TrackedLink(linkId, url, List.copyOf(cl.getTags()), List.copyOf(cl.getFilters()));
  }
}
