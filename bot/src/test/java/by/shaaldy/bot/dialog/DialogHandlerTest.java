package by.shaaldy.bot.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import by.shaaldy.bot.dialog.step.DialogStep;

/** Проверяет только диспетчеризацию: логика шагов тестируется в их собственных тестах. */
@ExtendWith(MockitoExtension.class)
class DialogHandlerTest {

  private static final long CHAT = 1L;

  @Mock DialogStateHolder holder;
  @Mock DialogStep modeStep;
  @Mock DialogStep digestHourStep;

  DialogHandler handler;

  @BeforeEach
  void setUp() {
    when(modeStep.state()).thenReturn(DialogState.AWAITING_MODE);
    when(digestHourStep.state()).thenReturn(DialogState.AWAITING_DIGEST_HOUR);
    handler = new DialogHandler(List.of(modeStep, digestHourStep), holder);
  }

  private DialogContext contextIn(DialogState state) {
    DialogContext ctx = new DialogContext();
    ctx.setState(state);
    when(holder.get(CHAT)).thenReturn(ctx);
    return ctx;
  }

  @Test
  void routesToStepMatchingCurrentState_andReturnsItsAnswer() {
    DialogContext ctx = contextIn(DialogState.AWAITING_MODE);
    when(modeStep.handle(CHAT, ctx, "instant")).thenReturn("Режим: сразу.");

    String result = handler.handle(CHAT, "instant");

    assertThat(result).isEqualTo("Режим: сразу.");
    verify(digestHourStep, never()).handle(anyLong(), any(), anyString());
  }

  @Test
  void routesEachStateToItsOwnStep() {
    DialogContext ctx = contextIn(DialogState.AWAITING_DIGEST_HOUR);
    when(digestHourStep.handle(CHAT, ctx, "10")).thenReturn("Режим: дайджест в 10:00.");

    String result = handler.handle(CHAT, "10");

    assertThat(result).isEqualTo("Режим: дайджест в 10:00.");
    verify(modeStep, never()).handle(anyLong(), any(), anyString());
  }

  @Test
  void idleState_returnsInactiveMessage_withoutCallingSteps() {
    contextIn(DialogState.IDLE);

    String result = handler.handle(CHAT, "что угодно");

    assertThat(result).contains("Диалог не активен");
    verify(modeStep, never()).handle(anyLong(), any(), anyString());
    verify(digestHourStep, never()).handle(anyLong(), any(), anyString());
  }

  @Test
  void twoStepsForSameState_failAtConstruction() {
    DialogStep duplicate = mock(DialogStep.class);
    when(duplicate.state()).thenReturn(DialogState.AWAITING_MODE);

    assertThatThrownBy(() -> new DialogHandler(List.of(modeStep, duplicate), holder))
        .isInstanceOf(IllegalStateException.class);
  }
}
