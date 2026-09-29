package by.shaaldy.bot.dialog.step;

import java.net.URI;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import by.shaaldy.bot.dialog.DialogContext;
import by.shaaldy.bot.dialog.DialogState;
import by.shaaldy.bot.dialog.DialogStateHolder;
import by.shaaldy.bot.dialog.utils.Messages;
import by.shaaldy.bot.dto.scrapper.TagRequest;
import by.shaaldy.bot.exception.ScrapperApiException;
import by.shaaldy.bot.service.cache.LinkQueryService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TagNameStep implements DialogStep {

  private final LinkQueryService linkQueryService;
  private final DialogStateHolder holder;

  @Override
  public DialogState state() {
    return DialogState.AWAITING_TAG_NAME;
  }

  @Override
  public String handle(long chatId, DialogContext ctx, String text) {
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
        return Messages.CHAT_NOT_FOUND;
      }
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
