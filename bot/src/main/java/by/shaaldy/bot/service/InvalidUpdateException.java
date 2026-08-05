package by.shaaldy.bot.service;

public class InvalidUpdateException extends RuntimeException {
  public InvalidUpdateException(String message) {
    super(message);
  }
}
