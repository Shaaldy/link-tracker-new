package by.shaaldy.scrapper.exception;

import java.net.URI;

public class LinkNotFoundException extends RuntimeException {
  public LinkNotFoundException(long chatId, URI url) {
    super("Ссылка не отслеживается чатом " + chatId + ": " + url);
  }
}
