package by.shaaldy.bot.telegram;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;

import by.shaaldy.bot.command.CommandDispatcher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class UpdateListener {
  private final TelegramBot telegramBot;
  private final MessageSender messageSender;
  private final CommandDispatcher commandDispatcher;

  @PostConstruct
  public void start() {
    telegramBot.setUpdatesListener(
        updates -> {
          updates.forEach(this::handle);
          return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    log.info("Telegram update listener started");
  }

  private void handle(Update update) {
    if (update.message() == null || update.message().text() == null) {
      return;
    }
    long chatId = update.message().chat().id();
    String text = update.message().text();
    log.info("Received message from chat {}: {}", chatId, text);

    String response = commandDispatcher.dispatch(chatId, text);
    messageSender.send(chatId, response);
  }
}
