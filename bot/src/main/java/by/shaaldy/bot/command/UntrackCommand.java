package by.shaaldy.bot.command;

import org.springframework.stereotype.Component;

import by.shaaldy.bot.dialog.DialogState;
import by.shaaldy.bot.dialog.DialogStateHolder;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UntrackCommand implements Command {

  private final DialogStateHolder holder;

  @Override
  public String command() {
    return "/untrack";
  }

  @Override
  public String description() {
    return "прекратить отслеживание ссылки";
  }

  @Override
  public String execute(long chatIt, String text) {
    holder.get(chatIt).setState(DialogState.AWAITING_UNTRACK);
    return "Введите ссылку, которую нужно удалить:";
  }
}
