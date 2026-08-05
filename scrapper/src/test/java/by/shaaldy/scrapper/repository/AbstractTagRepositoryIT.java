package by.shaaldy.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import by.shaaldy.scrapper.domain.TrackedLink;

/**
 * Общие интеграционные тесты управления тегами. Работают через интерфейс SubscriptionRepository,
 * поэтому один набор проверяет обе реализации — наследники лишь задают access-type. Обе ветки
 * обязаны вести себя одинаково за общим контрактом.
 */
public abstract class AbstractTagRepositoryIT extends AbstractPostgresIT {

  @Autowired SubscriptionRepository repository;

  private static final URI URL_A = URI.create("https://github.com/tag/a");
  private static final URI URL_B = URI.create("https://github.com/tag/b");

  /** Уникальный базовый chatId на реализацию, чтобы SQL и ORM прогоны не пересекались. */
  protected abstract long baseChatId();

  @Test
  void findLinksByChatAndTag_returnsOnlyLinksWithTag() {
    long chat = baseChatId() + 1;
    repository.registerChat(chat);
    repository.addLink(chat, URL_A, List.of("работа", "срочно"), List.of());
    repository.addLink(chat, URL_B, List.of("хобби"), List.of());

    List<TrackedLink> work = repository.findLinksByChatAndTag(chat, "работа");

    assertThat(work).hasSize(1);
    assertThat(work.getFirst().url()).isEqualTo(URL_A);
    assertThat(work.getFirst().tags()).contains("работа", "срочно");
  }

  @Test
  void findLinksByChatAndTag_noMatch_returnsEmpty() {
    long chat = baseChatId() + 2;
    repository.registerChat(chat);
    repository.addLink(chat, URL_A, List.of("хобби"), List.of());

    assertThat(repository.findLinksByChatAndTag(chat, "работа")).isEmpty();
  }

  @Test
  void findTagsByChat_returnsAllDistinctTags() {
    long chat = baseChatId() + 3;
    repository.registerChat(chat);
    repository.addLink(chat, URL_A, List.of("работа", "срочно"), List.of());
    repository.addLink(chat, URL_B, List.of("работа", "хобби"), List.of()); // "работа" дублируется

    Set<String> tags = repository.findTagsByChat(chat);

    assertThat(tags).containsExactlyInAnyOrder("работа", "срочно", "хобби"); // без дублей
  }

  @Test
  void addTag_newTag_appearsInSubscription() {
    long chat = baseChatId() + 4;
    repository.registerChat(chat);
    repository.addLink(chat, URL_A, List.of("старый"), List.of());

    boolean added = repository.addTag(chat, URL_A, "новый");

    assertThat(added).isTrue();
    assertThat(repository.findLinksByChat(chat).getFirst().tags())
        .containsExactlyInAnyOrder("старый", "новый");
  }

  @Test
  void addTag_duplicate_returnsFalse() {
    long chat = baseChatId() + 5;
    repository.registerChat(chat);
    repository.addLink(chat, URL_A, List.of("тег"), List.of());

    boolean added = repository.addTag(chat, URL_A, "тег"); // уже есть

    assertThat(added).isFalse();
  }

  @Test
  void addTag_noSubscription_returnsFalse() {
    long chat = baseChatId() + 6;
    repository.registerChat(chat);
    // подписки на URL_A нет

    assertThat(repository.addTag(chat, URL_A, "тег")).isFalse();
  }

  @Test
  void removeTag_existingTag_disappears() {
    long chat = baseChatId() + 7;
    repository.registerChat(chat);
    repository.addLink(chat, URL_A, List.of("оставить", "убрать"), List.of());

    boolean removed = repository.removeTag(chat, URL_A, "убрать");

    assertThat(removed).isTrue();
    assertThat(repository.findLinksByChat(chat).getFirst().tags()).containsExactly("оставить");
  }

  @Test
  void removeTag_onlyFromThisSubscription_notOthers() {
    long chat = baseChatId() + 8;
    repository.registerChat(chat);
    repository.addLink(chat, URL_A, List.of("общий"), List.of());
    repository.addLink(chat, URL_B, List.of("общий"), List.of());

    repository.removeTag(chat, URL_A, "общий"); // убрать только у A

    // у A тега нет, у B остался
    assertThat(repository.findLinksByChatAndTag(chat, "общий")).hasSize(1);
    assertThat(repository.findLinksByChatAndTag(chat, "общий").getFirst().url()).isEqualTo(URL_B);
  }

  @Test
  void removeTag_absentTag_returnsFalse() {
    long chat = baseChatId() + 9;
    repository.registerChat(chat);
    repository.addLink(chat, URL_A, List.of("есть"), List.of());

    assertThat(repository.removeTag(chat, URL_A, "нету")).isFalse();
  }

  /* --- изоляция тегов по чатам: тег принадлежит подписке (chat_id+link_id), не ссылке --- */

  @Test
  void findLinksByChatAndTag_tagOfAnotherChat_notVisible() {
    // Два чата на ОДНОЙ ссылке, тег только у первого. Второй по этому тегу ничего не видит.
    long chatX = baseChatId() + 10;
    long chatY = baseChatId() + 11;
    repository.registerChat(chatX);
    repository.registerChat(chatY);
    repository.addLink(chatX, URL_A, List.of("приватный"), List.of());
    repository.addLink(chatY, URL_A, List.of(), List.of()); // та же ссылка, без тега

    assertThat(repository.findLinksByChatAndTag(chatX, "приватный")).hasSize(1);
    assertThat(repository.findLinksByChatAndTag(chatY, "приватный")).isEmpty();
  }

  @Test
  void removeTag_doesNotAffectAnotherChatOnSameLink() {
    // Оба чата пометили общую ссылку одинаковым тегом. Удаление у X не трогает Y.
    long chatX = baseChatId() + 12;
    long chatY = baseChatId() + 13;
    repository.registerChat(chatX);
    repository.registerChat(chatY);
    repository.addLink(chatX, URL_A, List.of("общий"), List.of());
    repository.addLink(chatY, URL_A, List.of("общий"), List.of());

    repository.removeTag(chatX, URL_A, "общий");

    assertThat(repository.findLinksByChatAndTag(chatX, "общий")).isEmpty();
    assertThat(repository.findLinksByChatAndTag(chatY, "общий")).hasSize(1); // у Y остался
  }

  @Test
  void addTag_doesNotLeakToAnotherChatOnSameLink() {
    // Добавление тега чатом X на общую ссылку не появляется у чата Y.
    long chatX = baseChatId() + 14;
    long chatY = baseChatId() + 15;
    repository.registerChat(chatX);
    repository.registerChat(chatY);
    repository.addLink(chatX, URL_A, List.of(), List.of());
    repository.addLink(chatY, URL_A, List.of(), List.of());

    repository.addTag(chatX, URL_A, "личное");

    assertThat(repository.findTagsByChat(chatX)).contains("личное");
    assertThat(repository.findTagsByChat(chatY)).doesNotContain("личное");
  }

  @Test
  void findTagsByChat_emptyChat_returnsEmpty() {
    long chat = baseChatId() + 16;
    repository.registerChat(chat);

    assertThat(repository.findTagsByChat(chat)).isEmpty();
  }

  @Test
  void findTagsByChat_tagsOfAnotherChat_notIncluded() {
    long chatX = baseChatId() + 17;
    long chatY = baseChatId() + 18;
    repository.registerChat(chatX);
    repository.registerChat(chatY);
    repository.addLink(chatX, URL_A, List.of("тег-X"), List.of());
    repository.addLink(chatY, URL_B, List.of("тег-Y"), List.of());

    assertThat(repository.findTagsByChat(chatX)).containsExactly("тег-X");
    assertThat(repository.findTagsByChat(chatX)).doesNotContain("тег-Y");
  }

  @Test
  void removeLink_cascadesTags() {
    // Удаление подписки убирает её теги (каскад link_tags).
    long chat = baseChatId() + 19;
    repository.registerChat(chat);
    repository.addLink(chat, URL_A, List.of("уйдёт"), List.of());

    repository.removeLink(chat, URL_A);

    assertThat(repository.findTagsByChat(chat)).doesNotContain("уйдёт");
  }
}
