package by.shaaldy.bot.dialog.step;

import java.net.URI;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import by.shaaldy.bot.dialog.*;
import by.shaaldy.bot.dialog.utils.InputParser;
import by.shaaldy.bot.dialog.utils.Messages;
import by.shaaldy.bot.dto.scrapper.AddLinkRequest;
import by.shaaldy.bot.dto.scrapper.LinkResponse;
import by.shaaldy.bot.exception.ScrapperApiException;
import by.shaaldy.bot.service.cache.LinkQueryService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FiltersStep implements DialogStep {

  private final LinkQueryService linkQueryService;
  private final DialogStateHolder holder;

  @Override
  public DialogState state() {
    return DialogState.AWAITING_FILTERS;
  }

  @Override
  public String handle(long chatId, DialogContext ctx, String text) {
    List<String> tags = ctx.getTags() == null ? List.of() : ctx.getTags();
    try {
      AddLinkRequest request =
          new AddLinkRequest()
              .link(URI.create(ctx.getLink()))
              .tags(tags)
              .filters(InputParser.words(text));
      LinkResponse added = linkQueryService.addLink(chatId, request);
      return "Ссылка добавлена: " + added.getUrl();
    } catch (ScrapperApiException e) {
      if (e.getStatus().value() == 409) return "Эта ссылка уже отслеживается.";
      if (e.getStatus().value() == 404) return "Сначала зарегистрируйтесь: /start.";
      return e.userMessage();
    } catch (IllegalArgumentException e) {
      return Messages.INVALID_LINK;
    } catch (RestClientException e) {
      return Messages.SERVICE_UNAVAILABLE;
    } finally {
      holder.reset(chatId);
    }
  }
}
