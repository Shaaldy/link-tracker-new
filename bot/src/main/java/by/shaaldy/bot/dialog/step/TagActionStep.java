package by.shaaldy.bot.dialog.step;

import org.springframework.stereotype.Component;

import by.shaaldy.bot.dialog.DialogContext;
import by.shaaldy.bot.dialog.DialogState;

@Component
public class TagActionStep implements DialogStep {
  @Override
  public DialogState state() {
    return DialogState.AWAITING_TAG_ACTION;
  }

  @Override
  public String handle(long chatId, DialogContext ctx, String text) {
    String action = text.trim().toLowerCase();
    if (!action.equals("add") && !action.equals("remove")) {
      return "Введите add или remove.";
    }
    ctx.setTagAction(action);
    ctx.setState(DialogState.AWAITING_TAG_NAME);
    return "Введите тег:";
  }
}
