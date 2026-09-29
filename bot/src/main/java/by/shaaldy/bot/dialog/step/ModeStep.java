package by.shaaldy.bot.dialog.step;

import org.springframework.stereotype.Component;

import by.shaaldy.bot.dialog.DialogContext;
import by.shaaldy.bot.dialog.DialogState;
import by.shaaldy.bot.dialog.DialogStateHolder;
import by.shaaldy.bot.service.digest.NotificationMode;
import by.shaaldy.bot.service.digest.NotificationModeService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ModeStep implements DialogStep {

  private final DialogStateHolder holder;
  private final NotificationModeService modeService;

  @Override
  public DialogState state() {
    return DialogState.AWAITING_MODE;
  }

  @Override
  public String handle(long chatId, DialogContext ctx, String text) {
    String choice = text.strip().toLowerCase();
    return switch (choice) {
      case "instant", "сразу", "1" -> {
        holder.reset(chatId);
        yield modeService.apply(chatId, NotificationMode.INSTANT, null);
      }
      case "digest", "дайджест", "2" -> {
        ctx.setState(DialogState.AWAITING_DIGEST_HOUR);
        yield "Во сколько присылать дайджест? Введите час (0–23):";
      }
      default -> "Не понял. Введите «instant» или «digest» (либо 1 / 2):";
    };
  }
}
