package by.shaaldy.scrapper.repository.orm.jpa;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import by.shaaldy.scrapper.repository.orm.jpa.entity.ChatEntity;

public interface ChatJpaRepository extends JpaRepository<ChatEntity, Long> {
  @Query(
      "SELECT c.chatId FROM ChatEntity c WHERE c.notificationMode = 'DIGEST' AND c.digestHour = :hour")
  Set<Long> findDigestRecipients(@Param("hour") int hour);
}
