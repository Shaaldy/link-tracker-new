package by.shaaldy.bot.command;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import by.shaaldy.bot.client.ScrapperClient;
import by.shaaldy.bot.dialog.DialogContext;
import by.shaaldy.bot.dialog.DialogState;
import by.shaaldy.bot.dialog.DialogStateHolder;
import by.shaaldy.bot.dto.scrapper.ListLinksResponse;
import by.shaaldy.bot.exception.ScrapperApiException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TagCommand implements Command {

  private final ScrapperClient scrapperClient;
  private final DialogStateHolder holder;

  @Override
  public String command() {
    return "/tag";
  }

  @Override
  public String description() {
    return "управление тегами ссылки (добавить/убрать)";
  }

  @Override
  public String execute(long chatId, String text) {
    try {
      ListLinksResponse response = scrapperClient.listLinks(chatId);
      if (response.getSize() == null || response.getSize() == 0) {
        return "У вас нет отслеживаемых ссылок. Сначала добавьте: /track.";
      }

      List<String> urls = response.getLinks().stream().map(l -> l.getUrl().toString()).toList();

      // сохраняем список в контекст для резолва номер -> url на следующем шаге
      DialogContext ctx = holder.get(chatId);
      ctx.setLinkChoices(urls);
      ctx.setState(DialogState.AWAITING_TAG_LINK);

      String numbered =
          IntStream.range(0, urls.size())
              .mapToObj(i -> (i + 1) + ". " + urls.get(i))
              .collect(Collectors.joining("\n"));
      return "Выберите ссылку (введите номер):\n" + numbered;
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
