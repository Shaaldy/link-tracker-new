package by.shaaldy.bot.command;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import by.shaaldy.bot.exception.ScrapperApiException;
import by.shaaldy.bot.service.RegistrationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StartCommand implements Command {
  private final RegistrationService registrationService;

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
      boolean wasNew = registrationService.registerIfAbsent(chatId);
      return wasNew
          ? "Привет! Вы зарегистрированы. Наберите /help для списка команд."
          : "Вы уже зарегистрированы. Наберите /help.";
    } catch (ScrapperApiException e) {
      if (e.getStatus().value() == 409) {
        return "Вы уже зарегистрированы. Наберите /help.";
      }
      return e.userMessage();
    } catch (RestClientException e) {
      return "Сервис временно недоступен, попробуйте позже.";
    }
  }

  @Override
  public boolean requiresRegistration() {
    return false;
  }
}
