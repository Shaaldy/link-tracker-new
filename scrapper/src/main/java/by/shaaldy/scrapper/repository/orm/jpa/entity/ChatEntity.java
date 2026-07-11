package by.shaaldy.scrapper.repository.orm.jpa.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity таблицы {@code chats}. Ключ {@code chat_id} — реальный Telegram chat id (приходит извне,
 * уже уникален), поэтому НЕ @GeneratedValue.
 */
@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
public class ChatEntity {

  @Id
  @Column(name = "chat_id")
  private Long chatId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  public ChatEntity(Long chatId) {
    this.chatId = chatId;
  }
}
