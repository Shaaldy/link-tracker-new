package by.shaaldy.scrapper.repository.orm.jpa.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Составной первичный ключ подписки {@code (chat_id, link_id)}. Требования JPA к классу ключа:
 * Embeddable, Serializable, no-arg конструктор, equals/hashCode.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ChatLinkId implements Serializable {

  @Column(name = "chat_id")
  private Long chatId;

  @Column(name = "link_id")
  private Long linkId;

  public ChatLinkId(Long chatId, Long linkId) {
    this.chatId = chatId;
    this.linkId = linkId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ChatLinkId that)) {
      return false;
    }
    return Objects.equals(chatId, that.chatId) && Objects.equals(linkId, that.linkId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chatId, linkId);
  }
}
