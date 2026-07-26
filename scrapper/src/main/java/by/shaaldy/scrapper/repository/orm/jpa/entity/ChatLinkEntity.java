package by.shaaldy.scrapper.repository.orm.jpa.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity таблицы {@code chat_links} — подписка (центральная сущность). Составной ключ через {@link
 * ChatLinkId}. Теги и фильтры — коллекции значений в отдельных таблицах, привязанные к подписке
 * составным ключом (два @JoinColumn).
 *
 * <p>Внутренняя деталь ORM-реализации: конвертируется в TrackedLink на границе обёртки.
 * Теги/фильтры — LAZY, поэтому конвертация entity→домен обязана происходить внутри транзакции
 * обёртки (иначе LazyInitializationException). Две LAZY-коллекции также избегают
 * MultipleBagFetchException, который дали бы две EAGER-коллекции типа List.
 */
@Entity
@Table(name = "chat_links")
@Getter
@Setter
@NoArgsConstructor
public class ChatLinkEntity {

  @EmbeddedId private ChatLinkId id;

  @ElementCollection
  @CollectionTable(
      name = "link_tags",
      joinColumns = {
        @JoinColumn(name = "chat_id", referencedColumnName = "chat_id"),
        @JoinColumn(name = "link_id", referencedColumnName = "link_id")
      })
  @Column(name = "tag")
  private List<String> tags = new ArrayList<>();

  @ElementCollection
  @CollectionTable(
      name = "link_filters",
      joinColumns = {
        @JoinColumn(name = "chat_id", referencedColumnName = "chat_id"),
        @JoinColumn(name = "link_id", referencedColumnName = "link_id")
      })
  @Column(name = "filter")
  private List<String> filters = new ArrayList<>();

  public ChatLinkEntity(ChatLinkId id, List<String> tags, List<String> filters) {
    this.id = id;
    this.tags = new ArrayList<>(tags);
    this.filters = new ArrayList<>(filters);
  }
}
