package by.shaaldy.bot.dialog.step;

import java.net.URI;

import by.shaaldy.bot.dialog.utils.Messages;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import by.shaaldy.bot.dialog.*;
import by.shaaldy.bot.dto.scrapper.LinkResponse;
import by.shaaldy.bot.dto.scrapper.RemoveLinkRequest;
import by.shaaldy.bot.exception.ScrapperApiException;
import by.shaaldy.bot.service.cache.LinkQueryService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class UntrackStep implements DialogStep {

  private final LinkQueryService linkQueryService;
  private final DialogStateHolder holder;

  @Override
  public DialogState state() {
    return DialogState.AWAITING_UNTRACK;
  }

  @Override
  public String handle(long chatId, DialogContext ctx, String text) {
    try {
      RemoveLinkRequest request = new RemoveLinkRequest().link(URI.create(text.trim()));
      LinkResponse removed = linkQueryService.removeLink(chatId, request);
      return "Ссылка удалена: " + removed.getUrl();
    } catch (ScrapperApiException e) {
      if (e.getStatus().value() == 404) return Messages.LINK_NOT_TRACKED;
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
