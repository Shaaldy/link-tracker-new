package by.shaaldy.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import by.shaaldy.scrapper.domain.TrackedLink;

@TestPropertySource(properties = "app.access-type=ORM")
class OrmSubscriptionRepositoryIT extends AbstractPostgresIT {

  @Autowired SubscriptionRepository repository;

  private static final URI URL = URI.create("https://github.com/orm/repo");

  @Test
  void addLink_newSubscription_persistsWithTagsAndFilters() {
    repository.registerChat(1100L);

    TrackedLink added =
        repository.addLink(1100L, URL, List.of("работа", "важное"), List.of("type:pr"));

    assertThat(added.url()).isEqualTo(URL);
    assertThat(added.tags()).containsExactlyInAnyOrder("работа", "важное");
    assertThat(added.filters()).containsExactly("type:pr");

    List<TrackedLink> links = repository.findLinksByChat(1100L);
    assertThat(links).hasSize(1);
    assertThat(links.getFirst().tags()).containsExactlyInAnyOrder("работа", "важное");
  }

  @Test
  void removeLink_lastSubscriber_deletesOrphanLink() {
    repository.registerChat(1200L);
    repository.addLink(1200L, URL, List.of(), List.of());

    boolean removed = repository.removeLink(1200L, URL);

    assertThat(removed).isTrue();
    assertThat(repository.findLinksByChat(1200L)).isEmpty();
    assertThat(repository.subscriptionExists(1200L, URL)).isFalse();
  }

  @Test
  void findSubscribers_multipleChats_returnsAll() {
    repository.registerChat(1300L);
    repository.registerChat(1301L);
    repository.addLink(1300L, URL, List.of(), List.of());
    repository.addLink(1301L, URL, List.of(), List.of());

    Set<Long> subscribers = repository.findSubscribers(URL);

    assertThat(subscribers).containsExactlyInAnyOrder(1300L, 1301L);
  }

  @Test
  void removeChat_cascadesSubscriptions() {
    repository.registerChat(1400L);
    repository.addLink(1400L, URL, List.of("t"), List.of());

    boolean removed = repository.removeChat(1400L);

    assertThat(removed).isTrue();
    assertThat(repository.chatExists(1400L)).isFalse();
    assertThat(repository.findSubscribers(URL)).doesNotContain(1400L);
  }
}
