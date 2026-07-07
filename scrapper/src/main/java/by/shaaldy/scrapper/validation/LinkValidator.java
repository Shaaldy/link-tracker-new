package by.shaaldy.scrapper.validation;

import java.net.URI;

import org.springframework.stereotype.Component;

import by.shaaldy.scrapper.client.LinkSourceRouter;
import by.shaaldy.scrapper.exception.UnsupportedLinkException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LinkValidator {

  private final LinkSourceRouter router;

  public void validate(URI url) {
    if (!router.isSupported(url)) {
      throw new UnsupportedLinkException(url);
    }
  }
}
