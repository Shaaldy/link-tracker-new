package by.shaaldy.scrapper.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import by.shaaldy.scrapper.client.bot.BotClient;
import by.shaaldy.scrapper.client.LinkSourceRouter;
import by.shaaldy.scrapper.client.UpdateChecker;
import by.shaaldy.scrapper.domain.Link;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;
import by.shaaldy.scrapper.repository.SubscriptionRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateSchedulerTest {

    @Mock SubscriptionRepository repository;
    @Mock LinkSourceRouter router;
    @Mock BotClient botClient;
    @Mock UpdateChecker checker;

    @InjectMocks UpdateScheduler scheduler;

    private static final Instant FRESH = Instant.parse("2026-07-01T00:00:00Z");

    private Link link(long id, URI url) {
        return Link.builder().id(id).url(url).build();
    }

    @Test
    void poll_updatedLink_notifiesOnlyItsSubscribers() {
        URI url = URI.create("https://github.com/a/b");
        when(repository.findAllLinks()).thenReturn(List.of(link(1L, url)));
        when(router.route(url)).thenReturn(checker);
        when(checker.fetchLastActivity(url)).thenReturn(FRESH);
        when(repository.getCheckedAt(url)).thenReturn(Instant.EPOCH);
        when(repository.findSubscribers(url)).thenReturn(Set.of(10L, 20L));

        scheduler.poll();

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());
        assertThat(captor.getValue().getTgChatIds()).containsExactlyInAnyOrder(10L, 20L);
        verify(repository).updateCheckedAt(url, FRESH);
    }

    @Test
    void poll_nothingNewer_doesNotSend() {
        URI url = URI.create("https://github.com/a/b");
        when(repository.findAllLinks()).thenReturn(List.of(link(1L, url)));
        when(router.route(url)).thenReturn(checker);
        when(checker.fetchLastActivity(url)).thenReturn(FRESH);
        when(repository.getCheckedAt(url)).thenReturn(FRESH); // равно — не новее

        scheduler.poll();

        verify(botClient, never()).sendUpdate(any());
        verify(repository, never()).updateCheckedAt(any(), any());
    }

    @Test
    void poll_noSubscribers_movesCheckedAtWithoutSending() {
        URI url = URI.create("https://github.com/a/b");
        when(repository.findAllLinks()).thenReturn(List.of(link(1L, url)));
        when(router.route(url)).thenReturn(checker);
        when(checker.fetchLastActivity(url)).thenReturn(FRESH);
        when(repository.getCheckedAt(url)).thenReturn(Instant.EPOCH);
        when(repository.findSubscribers(url)).thenReturn(Set.of());

        scheduler.poll();

        verify(botClient, never()).sendUpdate(any());
        verify(repository).updateCheckedAt(url, FRESH);
    }

    @Test
    void poll_linkCheckFails_continuesWithOtherLinks() {
        URI bad = URI.create("https://github.com/broken/repo");
        URI good = URI.create("https://github.com/a/b");
        when(repository.findAllLinks()).thenReturn(List.of(link(1L, bad), link(2L, good)));
        when(router.route(bad)).thenReturn(checker);
        when(router.route(good)).thenReturn(checker);
        when(checker.fetchLastActivity(bad)).thenThrow(new RuntimeException("404"));
        when(checker.fetchLastActivity(good)).thenReturn(FRESH);
        when(repository.getCheckedAt(good)).thenReturn(Instant.EPOCH);
        when(repository.findSubscribers(good)).thenReturn(Set.of(10L));

        scheduler.poll();

        verify(botClient).sendUpdate(any());
        verify(repository).updateCheckedAt(good, FRESH);
    }
}