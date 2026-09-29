package by.shaaldy.bot.dialog.step;

import org.springframework.stereotype.Component;

import by.shaaldy.bot.dialog.DialogContext;
import by.shaaldy.bot.dialog.DialogState;
import by.shaaldy.bot.dialog.utils.InputParser;

@Component
public class TagsStep implements DialogStep {

  @Override
  public DialogState state() {
    return DialogState.AWAITING_TAGS;
  }

  @Override
  public String handle(long chatId, DialogContext ctx, String text) {
    ctx.setTags(InputParser.words(text));
    ctx.setState(DialogState.AWAITING_FILTERS);
    return "Введите фильтры (или - чтобы пропустить):";
  }
}
