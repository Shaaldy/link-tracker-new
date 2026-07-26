package by.shaaldy.scrapper.repository.orm.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import by.shaaldy.scrapper.repository.orm.jpa.entity.ChatEntity;

public interface ChatJpaRepository extends JpaRepository<ChatEntity, Long> {}
