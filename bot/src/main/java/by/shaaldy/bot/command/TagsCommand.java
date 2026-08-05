package by.shaaldy.bot.command;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import by.shaaldy.bot.client.ScrapperClient;
import by.shaaldy.bot.exception.ScrapperApiException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TagsCommand implements Command {
  private final ScrapperClient scrapperClient;

  @Override
  public String command() {
    return "/tags";
  }

  @Override
  public String description() {
    return "показать все теги";
  }

  @Override
  public String execute(long chatId, String text) {
    try {
      List<String> tags = scrapperClient.listTags(chatId);
      if (tags == null || tags.isEmpty()) {
        return "У вас пока нет тегов.";
      }
      return "Ваши теги:\n"
          + tags.stream().map(t -> "• " + t).collect(java.util.stream.Collectors.joining("\n"));
    } catch (ScrapperApiException e) {
      if (e.getStatus().value() == 404) {
        return "Сначала зарегистрируйтесь: /start.";
      }
      return e.userMessage();
    } catch (RestClientException e) {
      return "Сервис временно недоступен, попробуйте позже.";
    }
  }
}
