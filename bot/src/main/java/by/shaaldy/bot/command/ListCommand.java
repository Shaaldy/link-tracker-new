package by.shaaldy.bot.command;

public class ListCommand implements Command {
  @Override
  public String command() {
    return "/list";
  }

  @Override
  public String description() {
    return "показать отслеживаемые ссылки";
  }

  @Override
  public String execute(long chatId, String text) {
    // На шаге 5 заменим на реальный запрос к scrapper.
    return "Список отслеживаемых ссылок пуст.";
  }
}
