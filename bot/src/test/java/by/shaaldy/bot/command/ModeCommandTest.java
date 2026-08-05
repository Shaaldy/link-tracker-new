package by.shaaldy.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import by.shaaldy.bot.dialog.DialogContext;
import by.shaaldy.bot.dialog.DialogState;
import by.shaaldy.bot.dialog.DialogStateHolder;
import by.shaaldy.bot.service.digest.NotificationMode;
import by.shaaldy.bot.service.digest.NotificationModeService;

@ExtendWith(MockitoExtension.class)
class ModeCommandTest {

  @Mock DialogStateHolder holder;
  @Mock NotificationModeService modeService;

  ModeCommand command;

  private static final long CHAT = 1L;

  @BeforeEach
  void setUp() {
    command = new ModeCommand(holder, modeService);
  }

  @Test
  void execute_noArguments_startsDialogAtModeSelection() {
    DialogContext ctx = new DialogContext();
    when(holder.get(CHAT)).thenReturn(ctx);

    String result = command.execute(CHAT, "/mode");

    assertThat(ctx.getState()).isEqualTo(DialogState.AWAITING_MODE);
    assertThat(result).contains("1. instant", "2. digest");
  }

  @Test
  void execute_instantArgument_appliesDirectly() {
    when(modeService.apply(CHAT, NotificationMode.INSTANT, null)).thenReturn("Режим: сразу.");

    String result = command.execute(CHAT, "/mode instant");

    assertThat(result).isEqualTo("Режим: сразу.");
    verify(modeService).apply(CHAT, NotificationMode.INSTANT, null);
  }

  @Test
  void execute_digestWithHour_appliesDirectly() {
    when(modeService.parseHour("10")).thenReturn(10);
    when(modeService.apply(CHAT, NotificationMode.DIGEST, 10))
        .thenReturn("Режим: дайджест в 10:00.");

    String result = command.execute(CHAT, "/mode digest 10");

    assertThat(result).isEqualTo("Режим: дайджест в 10:00.");
    verify(modeService).apply(CHAT, NotificationMode.DIGEST, 10);
  }

  @Test
  void execute_digestWithInvalidHour_returnsErrorWithoutCallingService() {
    when(modeService.parseHour("25")).thenReturn(null);

    String result = command.execute(CHAT, "/mode digest 25");

    assertThat(result).contains("0 до 23");
    verify(modeService, org.mockito.Mockito.never()).apply(anyLong(), any(), any());
  }

  @Test
  void execute_digestWithoutHour_startsDialogAtHourSelection() {
    DialogContext ctx = new DialogContext();
    when(holder.get(CHAT)).thenReturn(ctx);

    String result = command.execute(CHAT, "/mode digest");

    assertThat(ctx.getState()).isEqualTo(DialogState.AWAITING_DIGEST_HOUR);
    assertThat(result).contains("Введите час");
  }

  @Test
  void execute_unknownMode_returnsUsageError() {
    String result = command.execute(CHAT, "/mode nonsense");

    assertThat(result).contains("Неизвестный режим");
  }
}
