package by.shaaldy.scrapper.repository.orm.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import by.shaaldy.scrapper.repository.orm.jpa.entity.ChatLinkEntity;
import by.shaaldy.scrapper.repository.orm.jpa.entity.ChatLinkId;

public interface ChatLinkJpaRepository extends JpaRepository<ChatLinkEntity, ChatLinkId> {

  /** Все подписки чата (для findLinksByChat). */
  List<ChatLinkEntity> findById_ChatId(Long chatId);

  /** Все подписки на ссылку (для findSubscribers — обратная навигация). */
  List<ChatLinkEntity> findById_LinkId(Long linkId);
}
