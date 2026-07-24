package by.shaaldy.bot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import by.shaaldy.bot.client.ScrapperClient;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTest {
  @Mock ScrapperClient scrapperClient;

  @Spy HashSet<Long> registered = new HashSet<>();

  @InjectMocks RegistrationService registrationService;

  private static final long CHAT = 1L;

  @Test
  void checkRegistration_alreadyCached_returnTrue() {
    registered.add(CHAT);

    boolean result = registrationService.isRegistered(CHAT);

    assertThat(result).isTrue();

    verify(scrapperClient, never()).existChat(anyLong());
  }

  @Test
  void checkRegistration_notCached_returnTrue() {
    when(scrapperClient.existChat(CHAT)).thenReturn(true);
    boolean result = registrationService.isRegistered(CHAT);
    assertThat(result).isTrue();
    verify(scrapperClient).existChat(CHAT);
    assertThat(registered).contains(CHAT);
  }

  @Test
  void checkRegistration_notCachedAndNotInDB_returnFalse() {
    when(scrapperClient.existChat(CHAT)).thenReturn(false);
    boolean result = registrationService.isRegistered(CHAT);
    assertThat(result).isFalse();
    verify(scrapperClient).existChat(CHAT);
    assertThat(registered).doesNotContain(CHAT);
  }
}
