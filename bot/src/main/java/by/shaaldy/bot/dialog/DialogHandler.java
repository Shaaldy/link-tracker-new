package by.shaaldy.bot.dialog;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DialogHandler {

  private static final String SKIP = "-";

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
    // TODO(stage-5): отправить ссылку/тэги/фильтры в scrapper через ScrapperClient
    String summary =
        "Ссылка добавлена (заглушка):\n"
            + "URL: "
            + ctx.getLink()
            + "\n"
            + "Тэги: "
            + ctx.getTags();
    holder.reset(chatId);
    return summary;
  }

  private String onUntrack(long chatId, String text) {
    // TODO(stage-5): удалить ссылку через ScrapperClient
    String result = "Ссылка удалена (заглушка): " + text.trim();
    holder.reset(chatId);
    return result;
  }
}
