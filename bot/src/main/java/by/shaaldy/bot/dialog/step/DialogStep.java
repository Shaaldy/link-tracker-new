package by.shaaldy.bot.dialog.step;

import by.shaaldy.bot.dialog.DialogContext;
import by.shaaldy.bot.dialog.DialogState;

public interface DialogStep {
  DialogState state();

  String handle(long chatId, DialogContext ctx, String text);
}
