package by.shaaldy.bot.telegram;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UpdateListener {
  private final TelegramBot telegramBot;
  private final MessageSender messageSender;
  private final MessageRouter messageRouter;
  private final Counter userMessagesCounter;

  public UpdateListener(
      TelegramBot telegramBot,
      MessageSender messageSender,
      MessageRouter messageRouter,
      MeterRegistry registry) {
    this.telegramBot = telegramBot;
    this.messageSender = messageSender;
    this.messageRouter = messageRouter;
    this.userMessagesCounter = registry.counter("bot.user.messages");
  }

  @EventListener(ApplicationReadyEvent.class)
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
    String response = messageRouter.route(chatId, text);
    messageSender.send(chatId, response);
  }
}
