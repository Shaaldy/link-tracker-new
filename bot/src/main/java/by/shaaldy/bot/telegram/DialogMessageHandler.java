package by.shaaldy.bot.telegram;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import by.shaaldy.bot.dialog.DialogHandler;
import by.shaaldy.bot.dialog.DialogStateHolder;
import lombok.RequiredArgsConstructor;

@Order(0)
@Component
@RequiredArgsConstructor
public class DialogMessageHandler implements MessageHandler {
  private final DialogStateHolder dialogStateHolder;
  private final DialogHandler dialogHandler;

  @Override
  public String handle(long chatId, String text) {
    return dialogHandler.handle(chatId, text);
  }

  @Override
  public boolean supports(long chatId) {
    return dialogStateHolder.isInDialog(chatId);
  }
}
