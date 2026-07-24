package by.shaaldy.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import by.shaaldy.scrapper.domain.TrackedLink;

@TestPropertySource(properties = "app.access-type=SQL")
class SqlSubscriptionRepositoryIT extends AbstractPostgresIT {

  @Autowired SubscriptionRepository repository;

  private static final URI URL = URI.create("https://github.com/a/b");

  @BeforeEach
  void clean() {
    repository.deleteAll();
  }

  @Test
  void addLink_newSubscription_persistsWithTagsAndFilters() {
    repository.registerChat(100L);

    TrackedLink added =
        repository.addLink(100L, URL, List.of("работа", "важное"), List.of("type:pr"));

    assertThat(added.url()).isEqualTo(URL);
    assertThat(added.tags()).containsExactlyInAnyOrder("работа", "важное");
    assertThat(added.filters()).containsExactly("type:pr");

    List<TrackedLink> links = repository.findLinksByChat(100L);
    assertThat(links).hasSize(1);
    assertThat(links.getFirst().tags()).containsExactlyInAnyOrder("работа", "важное");
  }

  @Test
  void removeLink_lastSubscriber_deletesOrphanLink() {
    repository.registerChat(200L);
    repository.addLink(200L, URL, List.of(), List.of());

    boolean removed = repository.removeLink(200L, URL);

    assertThat(removed).isTrue();
    assertThat(repository.findLinksByChat(200L)).isEmpty();
    // осиротевшая ссылка удалена — повторная подписка создаст её заново без ошибки
    assertThat(repository.subscriptionExists(200L, URL)).isFalse();
  }

  @Test
  void findSubscribers_multipleChats_returnsAll() {
    repository.registerChat(300L);
    repository.registerChat(301L);
    repository.addLink(300L, URL, List.of(), List.of());
    repository.addLink(301L, URL, List.of(), List.of());

    Set<Long> subscribers = repository.findSubscribers(URL);

    assertThat(subscribers).containsExactlyInAnyOrder(300L, 301L);
  }

  @Test
  void removeChat_cascadesSubscriptions() {
    repository.registerChat(400L);
    repository.addLink(400L, URL, List.of("t"), List.of());

    boolean removed = repository.removeChat(400L);

    assertThat(removed).isTrue();
    assertThat(repository.chatExists(400L)).isFalse();
    // подписка ушла каскадом
    assertThat(repository.findSubscribers(URL)).doesNotContain(400L);
  }
}
