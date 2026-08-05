package by.shaaldy.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import by.shaaldy.scrapper.domain.TrackedLink;
import by.shaaldy.scrapper.repository.SubscriptionRepository.SubscriberMode;

public abstract class AbstractSubscriptionRepositoryIT extends AbstractPostgresIT {

  @Autowired SubscriptionRepository repository;

  protected abstract long baseChatId();

  protected abstract URI url();

  private long chatId(long offset) {
    return baseChatId() + offset;
  }

  @Test
  void addLink_newSubscription_persistsWithTagsAndFilters() {
    long chatId = chatId(100);
    repository.registerChat(chatId);

    TrackedLink added =
        repository.addLink(chatId, url(), List.of("работа", "важное"), List.of("type:pr"));

    assertThat(added.url()).isEqualTo(url());
    assertThat(added.tags()).containsExactlyInAnyOrder("работа", "важное");
    assertThat(added.filters()).containsExactly("type:pr");

    List<TrackedLink> links = repository.findLinksByChat(chatId);
    assertThat(links).hasSize(1);
    assertThat(links.getFirst().tags()).containsExactlyInAnyOrder("работа", "важное");
  }

  @Test
  void removeLink_lastSubscriber_deletesOrphanLink() {
    long chatId = chatId(200);
    repository.registerChat(chatId);
    repository.addLink(chatId, url(), List.of(), List.of());

    boolean removed = repository.removeLink(chatId, url());

    assertThat(removed).isTrue();
    assertThat(repository.findLinksByChat(chatId)).isEmpty();
    assertThat(repository.subscriptionExists(chatId, url())).isFalse();
  }

  @Test
  void findSubscribers_multipleChats_returnsAll() {
    long chat1 = chatId(300);
    long chat2 = chatId(301);
    repository.registerChat(chat1);
    repository.registerChat(chat2);
    repository.addLink(chat1, url(), List.of(), List.of());
    repository.addLink(chat2, url(), List.of(), List.of());

    Set<Long> subscribers = repository.findSubscribers(url());

    assertThat(subscribers).containsExactlyInAnyOrder(chat1, chat2);
  }

  @Test
  void removeChat_cascadesSubscriptions() {
    long chatId = chatId(400);
    repository.registerChat(chatId);
    repository.addLink(chatId, url(), List.of("t"), List.of());

    boolean removed = repository.removeChat(chatId);

    assertThat(removed).isTrue();
    assertThat(repository.chatExists(chatId)).isFalse();
    assertThat(repository.findSubscribers(url())).doesNotContain(chatId);
  }

  @Test
  void findSubscribersWithMode_returnsCorrectModeForEachSubscriber() {
    long chat1 = chatId(500);
    long chat2 = chatId(501);
    repository.registerChat(chat1);
    repository.registerChat(chat2);
    repository.addLink(chat1, url(), List.of(), List.of());
    repository.addLink(chat2, url(), List.of(), List.of());

    repository.updateNotificationMode(chat1, "INSTANT", null);
    repository.updateNotificationMode(chat2, "DIGEST", 10);

    List<SubscriberMode> subscribers = repository.findSubscribersWithMode(url());

    assertThat(subscribers)
        .containsExactlyInAnyOrder(
            new SubscriberMode(chat1, "INSTANT"), new SubscriberMode(chat2, "DIGEST"));
  }

  @Test
  void findSubscribersWithMode_newChatWithoutModeChange_defaultsToInstant() {
    long chatId = chatId(600);
    repository.registerChat(chatId);
    repository.addLink(chatId, url(), List.of(), List.of());

    List<SubscriberMode> subscribers = repository.findSubscribersWithMode(url());

    assertThat(subscribers).containsExactly(new SubscriberMode(chatId, "INSTANT"));
  }
}
