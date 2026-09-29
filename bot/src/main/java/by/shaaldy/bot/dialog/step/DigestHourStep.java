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
public class DigestHourStep implements DialogStep {

  private final DialogStateHolder holder;
  private final NotificationModeService modeService;

  @Override
  public DialogState state() {
    return DialogState.AWAITING_DIGEST_HOUR;
  }

  @Override
  public String handle(long chatId, DialogContext ctx, String text) {
    Integer hour = modeService.parseHour(text);
    if (hour == null) {
      return "Час должен быть числом от 0 до 23. Попробуйте ещё раз:";
    }
    holder.reset(chatId);
    return modeService.apply(chatId, NotificationMode.DIGEST, hour);
  }
}
