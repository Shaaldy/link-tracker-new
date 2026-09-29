package by.shaaldy.bot.telegram;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import by.shaaldy.bot.command.CommandDispatcher;
import lombok.RequiredArgsConstructor;

@Order(1)
@Component
@RequiredArgsConstructor
public class CommandMessageHandler implements MessageHandler {
  private final CommandDispatcher commandDispatcher;

  @Override
  public String handle(long chatId, String text) {
    return commandDispatcher.dispatch(chatId, text);
  }

  @Override
  public boolean supports(long chatId) {
    // пока без логики
    return true;
  }
}
