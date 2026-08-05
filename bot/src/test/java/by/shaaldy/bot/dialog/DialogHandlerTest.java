package by.shaaldy.bot.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import by.shaaldy.bot.service.cache.LinkQueryService;
import by.shaaldy.bot.service.digest.NotificationMode;
import by.shaaldy.bot.service.digest.NotificationModeService;

@ExtendWith(MockitoExtension.class)
class DialogHandlerTest {

  @Mock LinkQueryService linkQueryService;
  @Mock DialogStateHolder holder;
  @Mock NotificationModeService modeService;

  DialogHandler handler;

  private static final long CHAT = 1L;

  @BeforeEach
  void setUp() {
    handler = new DialogHandler(linkQueryService, holder, modeService);
  }

  @Test
  void handle_idleState_returnsInactiveMessage() {
    DialogContext ctx = new DialogContext(); // дефолтный state = IDLE
    when(holder.get(CHAT)).thenReturn(ctx);

    String result = handler.handle(CHAT, "что угодно");

    assertThat(result).contains("Диалог не активен");
  }

  @Test
  void handle_awaitingMode_instantChoice_appliesInstantAndResetsDialog() {
    DialogContext ctx = new DialogContext();
    ctx.setState(DialogState.AWAITING_MODE);
    when(holder.get(CHAT)).thenReturn(ctx);
    when(modeService.apply(CHAT, NotificationMode.INSTANT, null)).thenReturn("Режим: сразу.");

    String result = handler.handle(CHAT, "instant");

    assertThat(result).isEqualTo("Режим: сразу.");
    verify(modeService).apply(CHAT, NotificationMode.INSTANT, null);
    verify(holder).reset(CHAT);
  }

  @Test
  void handle_awaitingMode_digestChoice_movesToHourStep() {
    DialogContext ctx = new DialogContext();
    ctx.setState(DialogState.AWAITING_MODE);
    when(holder.get(CHAT)).thenReturn(ctx);

    String result = handler.handle(CHAT, "digest");

    assertThat(ctx.getState()).isEqualTo(DialogState.AWAITING_DIGEST_HOUR);
    assertThat(result).contains("Введите час");
    verify(holder, never()).reset(CHAT);
  }

  @Test
  void handle_awaitingMode_numericChoice_worksAsAlias() {
    DialogContext ctx = new DialogContext();
    ctx.setState(DialogState.AWAITING_MODE);
    when(holder.get(CHAT)).thenReturn(ctx);

    String result = handler.handle(CHAT, "2");

    assertThat(ctx.getState()).isEqualTo(DialogState.AWAITING_DIGEST_HOUR);
  }

  @Test
  void handle_awaitingMode_unknownInput_returnsRetryMessage() {
    DialogContext ctx = new DialogContext();
    ctx.setState(DialogState.AWAITING_MODE);
    when(holder.get(CHAT)).thenReturn(ctx);

    String result = handler.handle(CHAT, "что-то непонятное");

    assertThat(result).contains("Не понял");
    assertThat(ctx.getState()).isEqualTo(DialogState.AWAITING_MODE);
  }

  @Test
  void handle_awaitingDigestHour_validHour_appliesDigestAndResetsDialog() {
    DialogContext ctx = new DialogContext();
    ctx.setState(DialogState.AWAITING_DIGEST_HOUR);
    when(holder.get(CHAT)).thenReturn(ctx);
    when(modeService.parseHour("10")).thenReturn(10);
    when(modeService.apply(CHAT, NotificationMode.DIGEST, 10))
        .thenReturn("Режим: дайджест в 10:00.");

    String result = handler.handle(CHAT, "10");

    assertThat(result).isEqualTo("Режим: дайджест в 10:00.");
    verify(modeService).apply(CHAT, NotificationMode.DIGEST, 10);
    verify(holder).reset(CHAT);
  }

  @Test
  void handle_awaitingDigestHour_invalidHour_retriesWithoutResettingDialog() {
    DialogContext ctx = new DialogContext();
    ctx.setState(DialogState.AWAITING_DIGEST_HOUR);
    when(holder.get(CHAT)).thenReturn(ctx);
    when(modeService.parseHour("25")).thenReturn(null);

    String result = handler.handle(CHAT, "25");

    assertThat(result).contains("0 до 23");
    verify(modeService, never()).apply(anyLong(), any(), any());
    verify(holder, never()).reset(CHAT);
  }
}
