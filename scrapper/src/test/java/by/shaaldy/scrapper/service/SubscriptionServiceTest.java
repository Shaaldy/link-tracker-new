package by.shaaldy.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import by.shaaldy.scrapper.domain.TrackedLink;
import by.shaaldy.scrapper.exception.ChatAlreadyExistsException;
import by.shaaldy.scrapper.exception.ChatNotFoundException;
import by.shaaldy.scrapper.exception.LinkAlreadyTrackedException;
import by.shaaldy.scrapper.exception.LinkNotFoundException;
import by.shaaldy.scrapper.exception.UnsupportedLinkException;
import by.shaaldy.scrapper.repository.SubscriptionRepository;
import by.shaaldy.scrapper.validation.LinkValidator;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock SubscriptionRepository repository;
    @Mock LinkValidator linkValidator;

    @InjectMocks SubscriptionService service;

    private static final long CHAT = 1L;
    private static final URI URL = URI.create("https://github.com/a/b");

    /* --- chats --- */

    @Test
    void registerChat_newChat_registers() {
        when(repository.registerChat(CHAT)).thenReturn(true);
        service.registerChat(CHAT);
        verify(repository).registerChat(CHAT);
    }

    @Test
    void registerChat_existingChat_throwChatAlreadyExists() {
        when(repository.registerChat(CHAT)).thenReturn(false);
        assertThatThrownBy(() -> service.registerChat(CHAT))
                .isInstanceOf(ChatAlreadyExistsException.class);
    }

    @Test
    void removeChat_existingChat_removes() {
        when(repository.removeChat(CHAT)).thenReturn(true);
        service.removeChat(CHAT);
        verify(repository).removeChat(CHAT);
    }

    @Test
    void removeChat_missingChat_throwChatNotFound() {
        when(repository.removeChat(CHAT)).thenReturn(false);
        assertThatThrownBy(() -> service.removeChat(CHAT))
                .isInstanceOf(ChatNotFoundException.class);
    }

    /* --- addLink --- */

    @Test
    void addLink_newSubscription_savesAndReturnsTrackedLink() {
        List<String> tags = List.of("t1");
        List<String> filters = List.of("f1");
        TrackedLink expected = new TrackedLink(1L, URL, tags, filters);
        when(repository.chatExists(CHAT)).thenReturn(true);
        when(repository.subscriptionExists(CHAT, URL)).thenReturn(false);
        when(repository.addLink(CHAT, URL, tags, filters)).thenReturn(expected);

        TrackedLink result = service.addLink(CHAT, URL, tags, filters);

        assertThat(result).isEqualTo(expected);
        verify(linkValidator).validate(URL);
    }

    @Test
    void addLink_alreadyTracked_throwLinkAlreadyTracked() {
        when(repository.chatExists(CHAT)).thenReturn(true);
        when(repository.subscriptionExists(CHAT, URL)).thenReturn(true);

        assertThatThrownBy(() -> service.addLink(CHAT, URL, List.of(), List.of()))
                .isInstanceOf(LinkAlreadyTrackedException.class);
        verify(repository, never()).addLink(anyLong(), any(), any(), any());
    }

    @Test
    void addLink_missingChat_throwChatNotFound() {
        when(repository.chatExists(CHAT)).thenReturn(false);

        assertThatThrownBy(() -> service.addLink(CHAT, URL, List.of(), List.of()))
                .isInstanceOf(ChatNotFoundException.class);
        verify(repository, never()).addLink(anyLong(), any(), any(), any());
    }

    @Test
    void addLink_unsupportedLink_throwUnsupportedLink() {
        URI bad = URI.create("https://gitlab.com/a/b");
        doThrow(new UnsupportedLinkException(bad)).when(linkValidator).validate(bad);

        assertThatThrownBy(() -> service.addLink(CHAT, bad, List.of(), List.of()))
                .isInstanceOf(UnsupportedLinkException.class);
        verify(repository, never()).chatExists(anyLong()); // validate падает раньше requireChat
        verify(repository, never()).addLink(anyLong(), any(), any(), any());
    }

    @Test
    void addLink_nullTagsAndFilters_normalizesToEmptyLists() {
        when(repository.chatExists(CHAT)).thenReturn(true);
        when(repository.subscriptionExists(CHAT, URL)).thenReturn(false);
        when(repository.addLink(eq(CHAT), eq(URL), any(), any()))
                .thenReturn(new TrackedLink(1L, URL, List.of(), List.of()));

        service.addLink(CHAT, URL, null, null);

        verify(repository).addLink(CHAT, URL, List.of(), List.of());
    }

    /* --- removeLink --- */

    @Test
    void removeLink_trackedLink_removesAndReturnsIt() {
        TrackedLink tracked = new TrackedLink(1L, URL, List.of(), List.of());
        when(repository.chatExists(CHAT)).thenReturn(true);
        when(repository.findLinksByChat(CHAT)).thenReturn(List.of(tracked));

        TrackedLink result = service.removeLink(CHAT, URL);

        assertThat(result).isEqualTo(tracked);
        verify(repository).removeLink(CHAT, URL);
    }

    @Test
    void removeLink_missingChat_throwChatNotFound() {
        when(repository.chatExists(CHAT)).thenReturn(false);

        assertThatThrownBy(() -> service.removeLink(CHAT, URL))
                .isInstanceOf(ChatNotFoundException.class);
        verify(repository, never()).removeLink(anyLong(), any());
    }

    @Test
    void removeLink_notTracked_throwLinkNotFound() {
        when(repository.chatExists(CHAT)).thenReturn(true);
        when(repository.findLinksByChat(CHAT)).thenReturn(List.of());

        assertThatThrownBy(() -> service.removeLink(CHAT, URL))
                .isInstanceOf(LinkNotFoundException.class);
        verify(repository, never()).removeLink(anyLong(), any());
    }

    /* --- getLinks --- */

    @Test
    void getLinks_existingChat_returnsLinks() {
        TrackedLink tracked = new TrackedLink(1L, URL, List.of(), List.of());
        when(repository.chatExists(CHAT)).thenReturn(true);
        when(repository.findLinksByChat(CHAT)).thenReturn(List.of(tracked));

        assertThat(service.getLinks(CHAT)).containsExactly(tracked);
    }

    @Test
    void getLinks_noSubscriptions_returnsEmptyList() {
        when(repository.chatExists(CHAT)).thenReturn(true);
        when(repository.findLinksByChat(CHAT)).thenReturn(List.of());

        assertThat(service.getLinks(CHAT)).isEmpty();
    }

    @Test
    void getLinks_missingChat_throwChatNotFound() {
        when(repository.chatExists(CHAT)).thenReturn(false);

        assertThatThrownBy(() -> service.getLinks(CHAT))
                .isInstanceOf(ChatNotFoundException.class);
        verify(repository, never()).findLinksByChat(anyLong());
    }
}