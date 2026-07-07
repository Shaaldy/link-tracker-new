package by.shaaldy.bot.command;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import by.shaaldy.bot.client.ScrapperApiException;
import by.shaaldy.bot.client.ScrapperClient;
import by.shaaldy.bot.dto.scrapper.ListLinksResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ListCommand implements Command {
  private final ScrapperClient scrapperClient;

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
    try {
      ListLinksResponse response = scrapperClient.listLinks(chatId);
      if (response.getSize() == null || response.getSize() == 0) {
        return "Список отслеживаемых ссылок пуст.";
      }
      return response.getLinks().stream()
          .map(
              l ->
                  "• "
                      + l.getUrl()
                      + (l.getTags().isEmpty() ? "" : "  тэги: " + String.join(", ", l.getTags()))
                      + (l.getFilters().isEmpty()
                          ? ""
                          : "  фильтры: " + String.join(", ", l.getFilters())))
          .collect(Collectors.joining("\n"));
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
