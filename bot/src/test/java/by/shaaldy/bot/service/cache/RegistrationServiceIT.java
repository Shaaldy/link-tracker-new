package by.shaaldy.bot.service.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import by.shaaldy.bot.client.ScrapperClient;
import by.shaaldy.bot.it.AbstractRedisIT;
import by.shaaldy.bot.service.RegistrationService;

class RegistrationServiceIT extends AbstractRedisIT {

  private static final long CHAT = 1L;

  @MockitoBean ScrapperClient scrapperClient;
  @Autowired RegistrationService registrationService;

  @Test
  void isRegistered_secondCall_servedFromCache() {
    when(scrapperClient.existChat(CHAT)).thenReturn(true);

    boolean first = registrationService.isRegistered(CHAT);
    boolean second = registrationService.isRegistered(CHAT);

    assertThat(first).isTrue();
    assertThat(second).isTrue();
    verify(scrapperClient, times(1)).existChat(CHAT); // второй раз — из кэша
  }

  @Test
  void isRegistered_notInDb_notCached_andQueriedAgain() {
    when(scrapperClient.existChat(CHAT)).thenReturn(false);

    registrationService.isRegistered(CHAT);
    registrationService.isRegistered(CHAT);

    assertThat(registrationService.isRegistered(CHAT)).isFalse();
    verify(scrapperClient, times(3))
        .existChat(CHAT); // false не кэшируется — каждый раз идём в scrapper
  }

  @Test
  void unregister_evictsCache_nextCallQueriesAgain() {
    when(scrapperClient.existChat(CHAT)).thenReturn(true);

    registrationService.isRegistered(CHAT); // populate
    registrationService.unregister(CHAT); // evict
    registrationService.isRegistered(CHAT); // снова к scrapper

    verify(scrapperClient, times(2)).existChat(CHAT);
  }

  @Test
  void registerIfAbsent_newChat_registersAndReturnsTrue() {
    when(scrapperClient.existChat(CHAT)).thenReturn(false);

    boolean result = registrationService.registerIfAbsent(CHAT);

    assertThat(result).isTrue();
    verify(scrapperClient).registerChat(CHAT);
  }
}
