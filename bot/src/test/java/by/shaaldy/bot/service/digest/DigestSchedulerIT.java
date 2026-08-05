package by.shaaldy.bot.service.digest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import by.shaaldy.bot.client.ScrapperClient;
import by.shaaldy.bot.it.AbstractRedisIT;
import by.shaaldy.bot.telegram.MessageSender;

class DigestSchedulerIT extends AbstractRedisIT {

  private static final long CHAT_A = 9001L;
  private static final long CHAT_B = 9002L;

  @MockitoBean ScrapperClient scrapperClient;
  @MockitoBean MessageSender messageSender;
  @Autowired DigestBuffer digestBuffer;
  @Autowired DigestScheduler digestScheduler;

  @Test
  void flush_sendsAccumulatedMessagesAndClearsBuffer() {
    digestBuffer.append(CHAT_A, "Обновление 1");
    digestBuffer.append(CHAT_A, "Обновление 2");
    when(scrapperClient.findDigestRecipients(anyInt())).thenReturn(List.of(CHAT_A));

    digestScheduler.flush();

    verify(messageSender).send(eq(CHAT_A), contains("Обновление 1"));
    assertThat(digestBuffer.get(CHAT_A)).isEmpty();
  }

  @Test
  void flush_emptyBuffer_doesNotSend() {
    when(scrapperClient.findDigestRecipients(anyInt())).thenReturn(List.of(CHAT_A));

    digestScheduler.flush();

    verify(messageSender, never()).send(anyLong(), any());
  }

  @Test
  void flush_multipleChats_sendsToEach() {
    digestBuffer.append(CHAT_A, "A-уведомление");
    digestBuffer.append(CHAT_B, "B-уведомление");
    when(scrapperClient.findDigestRecipients(anyInt())).thenReturn(List.of(CHAT_A, CHAT_B));

    digestScheduler.flush();

    verify(messageSender).send(eq(CHAT_A), contains("A-уведомление"));
    verify(messageSender).send(eq(CHAT_B), contains("B-уведомление"));
    assertThat(digestBuffer.get(CHAT_A)).isEmpty();
    assertThat(digestBuffer.get(CHAT_B)).isEmpty();
  }

  @Test
  void flush_noRecipients_doesNotCallMessageSender() {
    when(scrapperClient.findDigestRecipients(anyInt())).thenReturn(List.of());

    digestScheduler.flush();

    verify(messageSender, never()).send(anyLong(), any());
  }
}
