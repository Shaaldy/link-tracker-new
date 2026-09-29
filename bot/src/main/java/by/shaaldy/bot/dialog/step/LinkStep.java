package by.shaaldy.bot.dialog.step;

import org.springframework.stereotype.Component;

import by.shaaldy.bot.dialog.DialogContext;
import by.shaaldy.bot.dialog.DialogState;
import by.shaaldy.bot.dialog.utils.InputParser;
import by.shaaldy.bot.dialog.utils.Messages;

@Component
public class LinkStep implements DialogStep {
  @Override
  public DialogState state() {
    return DialogState.AWAITING_LINK;
  }

  @Override
  public String handle(long chatId, DialogContext ctx, String text) {
    return InputParser.absoluteHttpUri(text)
        .map(
            uri -> {
              ctx.setLink(uri.toString());
              ctx.setState(DialogState.AWAITING_TAGS);
              return "Введите тэги через пробел (или - чтобы пропустить):";
            })
        .orElse(Messages.INVALID_LINK);
  }
}
