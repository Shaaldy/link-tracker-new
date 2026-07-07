package by.shaaldy.bot.command;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import by.shaaldy.bot.client.ScrapperApiException;
import by.shaaldy.bot.client.ScrapperClient;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StartCommand implements Command {
  private final ScrapperClient scrapperClient;

  @Override
  public String command() {
    return "/start";
  }

  @Override
  public String description() {
    return "регистрация пользователя";
  }

  @Override
  public String execute(long chatId, String text) {
    try {
      scrapperClient.registerChat(chatId);
      return "Привет! Вы зарегистрированы. Наберите /help для списка команд.";
    } catch (ScrapperApiException e) {
      if (e.getStatus().value() == 409) {
        return "Вы уже зарегистрированы. Наберите /help.";
      }
      return e.userMessage();
    } catch (RestClientException e) {
      return "Сервис временно недоступен, попробуйте позже.";
    }
  }
}
