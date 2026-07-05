package by.shaaldy.scrapper.exception;

import java.net.URI;

public class UnsupportedLinkException extends RuntimeException {
    public UnsupportedLinkException(URI url) {
        super("Неподдерживаемая ссылка: " + url);
    }
}
