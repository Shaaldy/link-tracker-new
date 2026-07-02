package by.shaaldy.bot.dialog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class DialogStateHolder {
  private final Map<Long, DialogContext> contexts = new ConcurrentHashMap<>();

  public DialogContext get(long chatId) {
    return contexts.computeIfAbsent(chatId, id -> new DialogContext());
  }

  public boolean isInDialog(long chatId) {
    return get(chatId).getState() != DialogState.IDLE;
  }

  public void reset(long chatId) {
    contexts.put(chatId, new DialogContext());
  }
}
