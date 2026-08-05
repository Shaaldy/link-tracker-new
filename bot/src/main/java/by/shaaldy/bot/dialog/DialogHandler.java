package by.shaaldy.bot.dialog;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import by.shaaldy.bot.exception.ScrapperApiException;
import by.shaaldy.bot.dto.scrapper.AddLinkRequest;
import by.shaaldy.bot.dto.scrapper.LinkResponse;
import by.shaaldy.bot.dto.scrapper.RemoveLinkRequest;
import by.shaaldy.bot.dto.scrapper.TagRequest;
import by.shaaldy.bot.service.cache.LinkQueryService;
import by.shaaldy.bot.service.digest.NotificationMode;
import by.shaaldy.bot.service.digest.NotificationModeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DialogHandler {

  private static final String SKIP = "-";

  private final LinkQueryService linkQueryService;
  private final DialogStateHolder holder;
  private final NotificationModeService modeService;

  public String handle(long chatId, String text) {
    DialogContext ctx = holder.get(chatId);
    return switch (ctx.getState()) {
      case AWAITING_LINK -> onLink(ctx, text);
      case AWAITING_TAGS -> onTags(ctx, text);
      case AWAITING_FILTERS -> onFilters(chatId, ctx, text);
      case AWAITING_UNTRACK -> onUntrack(chatId, text);
      case AWAITING_TAG_LINK -> onTagLink(ctx, text);
      case AWAITING_TAG_ACTION -> onTagAction(ctx, text);
      case AWAITING_TAG_NAME -> onTagName(chatId, ctx, text);
      case AWAITING_MODE -> onMode(chatId, ctx, text);
      case AWAITING_DIGEST_HOUR -> onDigestHour(chatId, text);
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
      LinkResponse added = linkQueryService.addLink(chatId, request);
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
      LinkResponse removed = linkQueryService.removeLink(chatId, request);
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

  private String onTagLink(DialogContext ctx, String text) {
    int index;
    try {
      index = Integer.parseInt(text.trim()) - 1; // нумерация с 1
    } catch (NumberFormatException e) {
      return "Введите номер ссылки из списка.";
    }
    List<String> choices = ctx.getLinkChoices();
    if (choices == null || index < 0 || index >= choices.size()) {
      return "Нет ссылки с таким номером. Введите номер из списка.";
    }
    ctx.setSelectedUrl(choices.get(index));
    ctx.setState(DialogState.AWAITING_TAG_ACTION);
    return "Что сделать с тегом? Введите add (добавить) или remove (убрать):";
  }

  private String onTagAction(DialogContext ctx, String text) {
    String action = text.trim().toLowerCase();
    if (!action.equals("add") && !action.equals("remove")) {
      return "Введите add или remove.";
    }
    ctx.setTagAction(action);
    ctx.setState(DialogState.AWAITING_TAG_NAME);
    return "Введите тег:";
  }

  private String onTagName(long chatId, DialogContext ctx, String text) {
    String tag = text.trim();
    try {
      if (tag.isBlank()) {
        return "Тег не может быть пустым.";
      }
      TagRequest request = new TagRequest().url(URI.create(ctx.getSelectedUrl())).tag(tag);
      if (ctx.getTagAction().equals("add")) {
        linkQueryService.addTag(chatId, request);
        return "Тег «" + tag + "» добавлен к " + ctx.getSelectedUrl();
      } else {
        linkQueryService.removeTag(chatId, request);
        return "Тег «" + tag + "» убран у " + ctx.getSelectedUrl();
      }
    } catch (ScrapperApiException e) {
      if (e.getStatus().value() == 404) {
        return "Ссылка или чат не найдены.";
      }
      return e.userMessage();
    } catch (IllegalArgumentException e) {
      return "Некорректная ссылка.";
    } catch (RestClientException e) {
      return "Сервис временно недоступен, попробуйте позже.";
    } finally {
      holder.reset(chatId);
    }
  }

  private String onMode(long chatId, DialogContext ctx, String text) {
    String choice = text.strip().toLowerCase();
    return switch (choice) {
      case "instant", "сразу", "1" -> {
        holder.reset(chatId);
        yield modeService.apply(chatId, NotificationMode.INSTANT, null);
      }
      case "digest", "дайджест", "2" -> {
        ctx.setState(DialogState.AWAITING_DIGEST_HOUR);
        yield "Во сколько присылать дайджест? Введите час (0–23):";
      }
      default -> "Не понял. Введите «instant» или «digest» (либо 1 / 2):";
    };
  }

  private String onDigestHour(long chatId, String text) {
    Integer hour = modeService.parseHour(text);
    if (hour == null) {
      return "Час должен быть числом от 0 до 23. Попробуйте ещё раз:";
    }
    holder.reset(chatId);
    return modeService.apply(chatId, NotificationMode.DIGEST, hour);
  }
}
