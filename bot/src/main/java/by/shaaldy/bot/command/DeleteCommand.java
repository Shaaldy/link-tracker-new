package by.shaaldy.bot.command;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import by.shaaldy.bot.client.ScrapperApiException;
import by.shaaldy.bot.client.ScrapperClient;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DeleteCommand implements Command {
  private final ScrapperClient scrapperClient;

  @Override
  public String command() {
    return "/delete";
  }

  @Override
  public String description() {
    return "Удалить чат и все подписки";
  }

  @Override
  public String execute(long chatId, String text) {
    try {
      scrapperClient.deleteChat(chatId);
      return "Чат и все подписки успешно удалены";
    } catch (ScrapperApiException e) {
      if (e.getStatus().value() == 404) {
        return "Не удалось удалить: чат %d не зарегистрирован".formatted(chatId);
      }
      return "Не удалось удалить чат: %s".formatted(e.getMessage());
    } catch (RestClientException e) {
      return "Сервис временно недоступен, попробуйте позже.";
    }
  }
}
