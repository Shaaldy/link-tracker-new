package by.shaaldy.bot.dialog;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import by.shaaldy.bot.client.ScrapperApiException;
import by.shaaldy.bot.client.ScrapperClient;
import by.shaaldy.bot.dto.scrapper.AddLinkRequest;
import by.shaaldy.bot.dto.scrapper.LinkResponse;
import by.shaaldy.bot.dto.scrapper.RemoveLinkRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DialogHandler {

  private static final String SKIP = "-";

  private final ScrapperClient scrapperClient;
  private final DialogStateHolder holder;

  public String handle(long chatId, String text) {
    DialogContext ctx = holder.get(chatId);
    return switch (ctx.getState()) {
      case AWAITING_LINK -> onLink(ctx, text);
      case AWAITING_TAGS -> onTags(ctx, text);
      case AWAITING_FILTERS -> onFilters(chatId, ctx, text);
      case AWAITING_UNTRACK -> onUntrack(chatId, text);
      case IDLE -> "Диалог не активен. Наберите /help.";
    };
  }

  private String onLink(DialogContext ctx, String text) {
    ctx.setLink(text.trim());
    ctx.setState(DialogState.AWAITING_TAGS);
    return "Введите тэги через пробел (или - чтобы пропустить):";
  }

  private String onTags(DialogContext ctx, String text) {
    if (!text.trim().equals(SKIP)) {
      ctx.setTags(Arrays.stream(text.trim().split("\\s+")).toList());
    }
    ctx.setState(DialogState.AWAITING_FILTERS);
    return "Введите фильтры (или - чтобы пропустить):";
  }

  private String onFilters(long chatId, DialogContext ctx, String text) {
    List<String> filters =
        text.trim().equals(SKIP) ? List.of() : Arrays.stream(text.trim().split("\\s+")).toList();
    List<String> tags = ctx.getTags() == null ? List.of() : ctx.getTags();
    try {
      AddLinkRequest request =
          new AddLinkRequest().link(URI.create(ctx.getLink())).tags(tags).filters(filters);
      LinkResponse added = scrapperClient.addLink(chatId, request);
      return "Ссылка добавлена: " + added.getUrl();
    } catch (ScrapperApiException e) {
      if (e.getStatus().value() == 409) return "Эта ссылка уже отслеживается.";
      if (e.getStatus().value() == 404) return "Сначала зарегистрируйтесь: /start.";
      return e.userMessage();
    } catch (IllegalArgumentException e) {
      return "Некорректная ссылка.";
    } catch (RestClientException e) {
      return "Сервис временно недоступен, попробуйте позже.";
    } finally {
      holder.reset(chatId);
    }
  }

  private String onUntrack(long chatId, String text) {
    try {
      RemoveLinkRequest request = new RemoveLinkRequest().link(URI.create(text.trim()));
      LinkResponse removed = scrapperClient.removeLink(chatId, request);
      return "Ссылка удалена: " + removed.getUrl();
    } catch (ScrapperApiException e) {
      if (e.getStatus().value() == 404) return "Эта ссылка не отслеживается.";
      return e.userMessage();
    } catch (IllegalArgumentException e) {
      return "Некорректная ссылка.";
    } catch (RestClientException e) {
      return "Сервис временно недоступен, попробуйте позже.";
    } finally {
      holder.reset(chatId);
    }
  }
}
