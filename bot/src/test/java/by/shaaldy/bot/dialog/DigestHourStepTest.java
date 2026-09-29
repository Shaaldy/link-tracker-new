package by.shaaldy.bot.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import by.shaaldy.bot.dialog.step.DigestHourStep;
import by.shaaldy.bot.service.digest.NotificationMode;
import by.shaaldy.bot.service.digest.NotificationModeService;

@ExtendWith(MockitoExtension.class)
class DigestHourStepTest {

  private static final long CHAT = 1L;

  @Mock DialogStateHolder holder;
  @Mock NotificationModeService modeService;

  @InjectMocks DigestHourStep step;

  private static DialogContext context() {
    DialogContext ctx = new DialogContext();
    ctx.setState(DialogState.AWAITING_DIGEST_HOUR);
    return ctx;
  }

  @Test
  void handlesAwaitingDigestHourState() {
    assertThat(step.state()).isEqualTo(DialogState.AWAITING_DIGEST_HOUR);
  }

  @Test
  void validHour_appliesDigest_andResetsDialog() {
    when(modeService.parseHour("10")).thenReturn(10);
    when(modeService.apply(CHAT, NotificationMode.DIGEST, 10))
        .thenReturn("Режим: дайджест в 10:00.");

    String result = step.handle(CHAT, context(), "10");

    assertThat(result).isEqualTo("Режим: дайджест в 10:00.");
    verify(holder).reset(CHAT);
  }

  @Test
  void invalidHour_asksAgain_withoutApplyingOrResetting() {
    DialogContext ctx = context();
    when(modeService.parseHour("25")).thenReturn(null);

    String result = step.handle(CHAT, ctx, "25");

    assertThat(result).contains("0 до 23");
    assertThat(ctx.getState()).isEqualTo(DialogState.AWAITING_DIGEST_HOUR);
    verify(modeService, never()).apply(anyLong(), any(), any());
    verify(holder, never()).reset(anyLong());
  }
}
