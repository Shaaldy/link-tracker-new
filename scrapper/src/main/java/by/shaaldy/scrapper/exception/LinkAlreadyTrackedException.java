package by.shaaldy.scrapper.exception;

import java.net.URI;

public class LinkAlreadyTrackedException extends RuntimeException {
  public LinkAlreadyTrackedException(long chatId, URI url) {
    super("Ссылка уже отслеживается чатом " + chatId + ": " + url);
  }
}
