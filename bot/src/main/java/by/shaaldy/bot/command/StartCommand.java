package by.shaaldy.bot.command;

import org.springframework.stereotype.Component;

@Component
public class StartCommand implements Command {
  @Override
  public String command() {
    return "/start";
  }

  @Override
  public String description() {
    return "регистрация пользователя";
  }

  @Override
  public String execute(long chatIt, String text) {
    return "Привет! Вы зарегистрированы. Наберите /help для списка команд.";
  }
}
