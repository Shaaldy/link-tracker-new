package by.shaaldy.scrapper.client;

import java.net.URI;
import java.util.List;

import org.springframework.stereotype.Component;

import by.shaaldy.scrapper.exception.UnsupportedLinkException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LinkSourceRouter {

  private final List<UpdateChecker> checkers;

  public boolean isSupported(URI url) {
    return checkers.stream().anyMatch(checker -> checker.supports(url));
  }

  public UpdateChecker route(URI url) {
    return checkers.stream()
        .filter(checker -> checker.supports(url))
        .findFirst()
        .orElseThrow(() -> new UnsupportedLinkException(url));
  }
}
