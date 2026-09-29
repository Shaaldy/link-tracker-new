package by.shaaldy.bot.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import by.shaaldy.bot.dialog.step.ModeStep;
import by.shaaldy.bot.service.digest.NotificationMode;
import by.shaaldy.bot.service.digest.NotificationModeService;

@ExtendWith(MockitoExtension.class)
class ModeStepTest {

  private static final long CHAT = 1L;

  @Mock DialogStateHolder holder;
  @Mock NotificationModeService modeService;

  @InjectMocks ModeStep step;

  private static DialogContext context() {
    DialogContext ctx = new DialogContext();
    ctx.setState(DialogState.AWAITING_MODE);
    return ctx;
  }

  @Test
  void handlesAwaitingModeState() {
    assertThat(step.state()).isEqualTo(DialogState.AWAITING_MODE);
  }

  @ParameterizedTest
  @ValueSource(strings = {"instant", "сразу", "1", "  INSTANT "})
  void instantAliases_applyInstant_andResetDialog(String input) {
    when(modeService.apply(CHAT, NotificationMode.INSTANT, null)).thenReturn("Режим: сразу.");

    String result = step.handle(CHAT, context(), input);

    assertThat(result).isEqualTo("Режим: сразу.");
    verify(holder).reset(CHAT);
  }

  @ParameterizedTest
  @ValueSource(strings = {"digest", "дайджест", "2", " Digest "})
  void digestAliases_moveToHourStep_withoutApplyingOrResetting(String input) {
    DialogContext ctx = context();

    String result = step.handle(CHAT, ctx, input);

    assertThat(ctx.getState()).isEqualTo(DialogState.AWAITING_DIGEST_HOUR);
    assertThat(result).contains("Введите час");
    verifyNoInteractions(modeService);
    verify(holder, never()).reset(anyLong());
  }

  @Test
  void unknownInput_asksAgain_keepingState() {
    DialogContext ctx = context();

    String result = step.handle(CHAT, ctx, "что-то непонятное");

    assertThat(result).contains("Не понял");
    assertThat(ctx.getState()).isEqualTo(DialogState.AWAITING_MODE);
    verifyNoInteractions(modeService);
    verify(holder, never()).reset(anyLong());
  }
}
