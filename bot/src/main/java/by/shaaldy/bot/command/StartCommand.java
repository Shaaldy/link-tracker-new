package by.shaaldy.bot.command;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import by.shaaldy.bot.client.ScrapperApiException;
import by.shaaldy.bot.client.ScrapperClient;
import by.shaaldy.bot.service.RegistrationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StartCommand implements Command {
  private final ScrapperClient scrapperClient;
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
      scrapperClient.registerChat(chatId);
      registrationService.markRegistered(chatId);
      return "Привет! Вы зарегистрированы. Наберите /help для списка команд.";
    } catch (ScrapperApiException e) {
      if (e.getStatus().value() == 409) {
        registrationService.markRegistered(chatId);
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
