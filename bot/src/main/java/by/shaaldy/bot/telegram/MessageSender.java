package by.shaaldy.bot.telegram;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSender {

  private final TelegramBot telegramBot;

  public void send(long chatId, String text) {
    SendResponse response = telegramBot.execute(new SendMessage(chatId, text));
    if (!response.isOk()) {
      log.error("Failed to send message to chat {}: code={}", chatId, response.errorCode());
    }
  }
}
