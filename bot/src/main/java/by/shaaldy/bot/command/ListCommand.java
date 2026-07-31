package by.shaaldy.bot.command;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import by.shaaldy.bot.client.ScrapperApiException;
import by.shaaldy.bot.dto.scrapper.ListLinksResponse;
import by.shaaldy.bot.service.LinkQueryService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ListCommand implements Command {
  private final LinkQueryService linkQueryService;

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
    String[] parts = text.split("\\s+", 2);
    String tag = parts.length > 1 ? parts[1].strip() : null;

    try {
      ListLinksResponse response =
          (tag == null || tag.isBlank())
              ? linkQueryService.listLinks(chatId)
              : linkQueryService.listLinksByTag(chatId, tag);

      if (response.getSize() == null || response.getSize() == 0) {
        return (tag == null || tag.isBlank())
            ? "Список отслеживаемых ссылок пуст."
            : "Нет ссылок с тегом «" + tag + "».";
      }

      String header = (tag == null || tag.isBlank()) ? "" : "Ссылки с тегом «" + tag + "»:\n";
      return header
          + response.getLinks().stream()
              .map(
                  l ->
                      "• "
                          + l.getUrl()
                          + (l.getTags().isEmpty()
                              ? ""
                              : "  тэги: " + String.join(", ", l.getTags()))
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
