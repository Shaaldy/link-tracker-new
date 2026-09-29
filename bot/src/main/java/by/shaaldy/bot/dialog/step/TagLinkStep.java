package by.shaaldy.bot.dialog.step;

import java.util.List;

import org.springframework.stereotype.Component;

import by.shaaldy.bot.dialog.DialogContext;
import by.shaaldy.bot.dialog.DialogState;

@Component
public class TagLinkStep implements DialogStep {
  @Override
  public DialogState state() {
    return DialogState.AWAITING_TAG_LINK;
  }

  @Override
  public String handle(long chatId, DialogContext ctx, String text) {
    int index;
    try {
      index = Integer.parseInt(text.trim()) - 1; // нумерация с 1
    } catch (NumberFormatException e) {
      return "Введите номер ссылки из списка.";
    }
    List<String> choices = ctx.getLinkChoices();
    if (choices == null || index < 0 || index >= choices.size()) {
      return "Нет ссылки с таким номером. Введите номер из списка.";
    }
    ctx.setSelectedUrl(choices.get(index));
    ctx.setState(DialogState.AWAITING_TAG_ACTION);
    return "Что сделать с тегом? Введите add (добавить) или remove (убрать):";
  }
}
