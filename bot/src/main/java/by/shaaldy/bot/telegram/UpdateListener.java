package by.shaaldy.bot.telegram;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;

import by.shaaldy.bot.command.CommandDispatcher;
import by.shaaldy.bot.dialog.DialogHandler;
import by.shaaldy.bot.dialog.DialogStateHolder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UpdateListener {
  private final TelegramBot telegramBot;
  private final MessageSender messageSender;
  private final CommandDispatcher commandDispatcher;
  private final DialogStateHolder dialogStateHolder;
  private final DialogHandler dialogHandler;
  private final Counter userMessagesCounter;

  public UpdateListener(
      TelegramBot telegramBot,
      MessageSender messageSender,
      CommandDispatcher commandDispatcher,
      DialogStateHolder dialogStateHolder,
      DialogHandler dialogHandler,
      MeterRegistry registry) {
    this.telegramBot = telegramBot;
    this.messageSender = messageSender;
    this.commandDispatcher = commandDispatcher;
    this.dialogStateHolder = dialogStateHolder;
    this.dialogHandler = dialogHandler;
    this.userMessagesCounter = registry.counter("bot.user.messages");
  }

  @PostConstruct
  public void start() {
    telegramBot.setUpdatesListener(
        updates -> {
          updates.forEach(this::handle);
          return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    log.info("Telegram update listener started");
  }

  protected void handle(Update update) {
    if (update.message() == null || update.message().text() == null) {
      return;
    }
    long chatId = update.message().chat().id();
    String text = update.message().text();
    log.info("Received message from chat {}: {}", chatId, text);

    userMessagesCounter.increment();

    String response =
        dialogStateHolder.isInDialog(chatId)
            ? dialogHandler.handle(chatId, text)
            : commandDispatcher.dispatch(chatId, text);
    messageSender.send(chatId, response);
  }
}
