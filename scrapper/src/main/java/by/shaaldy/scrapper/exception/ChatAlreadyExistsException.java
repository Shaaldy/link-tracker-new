package by.shaaldy.scrapper.exception;

public class ChatAlreadyExistsException extends RuntimeException {
  public ChatAlreadyExistsException(long chatId) {
    super("Чат уже зарегистрирован: " + chatId);
  }
}
