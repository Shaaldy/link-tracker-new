package by.shaaldy.scrapper.exception;

public class ChatNotFoundException extends RuntimeException {
  public ChatNotFoundException(long chatId) {
    super("Чат не найден: " + chatId);
  }
}
