package by.shaaldy.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import by.shaaldy.scrapper.domain.TrackedLink;

class InMemorySubscriptionRepositoryTest {

  private SubscriptionRepository repository;

  private static final long CHAT = 1L;
  private static final URI URL = URI.create("https://github.com/a/b");

  @BeforeEach
  void setUp() {
    repository = new InMemorySubscriptionRepository();
  }

  @Test
  void registerChat_sameChatTwice_secondReturnsFalse() {
    assertThat(repository.registerChat(CHAT)).isTrue();
    assertThat(repository.registerChat(CHAT)).isFalse();
    assertThat(repository.chatExists(CHAT)).isTrue();
  }

  @Test
  void addLink_afterAdd_existsAndListed() {
    repository.registerChat(CHAT);

    TrackedLink added = repository.addLink(CHAT, URL, List.of(), List.of());

    assertThat(added.url()).isEqualTo(URL);
    assertThat(repository.subscriptionExists(CHAT, URL)).isTrue();
    assertThat(repository.findLinksByChat(CHAT)).extracting(TrackedLink::url).containsExactly(URL);
  }

  @Test
  void removeLink_afterAdd_subscriptionGone() {
    repository.registerChat(CHAT);
    repository.addLink(CHAT, URL, List.of(), List.of());

    repository.removeLink(CHAT, URL);

    assertThat(repository.subscriptionExists(CHAT, URL)).isFalse();
    assertThat(repository.findLinksByChat(CHAT)).isEmpty();
  }

  @Test
  void removeChat_afterRegister_chatGone() {
    repository.registerChat(CHAT);

    assertThat(repository.removeChat(CHAT)).isTrue();
    assertThat(repository.chatExists(CHAT)).isFalse();
    assertThat(repository.removeChat(CHAT)).isFalse();
  }

  @Test
  void addLink_withTagsAndFilters_storesThemOnSubscription() {
    repository.registerChat(CHAT);
    List<String> tags = List.of("java", "spring");
    List<String> filters = List.of("user=me");

    repository.addLink(CHAT, URL, tags, filters);

    TrackedLink stored = repository.findLinksByChat(CHAT).getFirst();
    assertThat(stored.url()).isEqualTo(URL);
    assertThat(stored.tags()).containsExactlyInAnyOrderElementsOf(tags);
    assertThat(stored.filters()).containsExactlyInAnyOrderElementsOf(filters);
  }
}
