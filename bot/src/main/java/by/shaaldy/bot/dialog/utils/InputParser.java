package by.shaaldy.bot.dialog.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

public final class InputParser {
  private static final String SKIP = "-";

  private InputParser() {}

  public static List<String> words(String text) {
    String t = text.strip();
    return t.equals(SKIP) || t.isEmpty() ? List.of() : List.of(t.split("\\s+"));
  }

  public static Optional<URI> absoluteHttpUri(String text) {
    try {
      URI uri = new URI(text.strip());
      boolean ok =
          uri.getHost() != null
              && ("http".equalsIgnoreCase(uri.getScheme())
                  || "https".equalsIgnoreCase(uri.getScheme()));
      return ok ? Optional.of(uri) : Optional.empty();
    } catch (URISyntaxException e) {
      return Optional.empty();
    }
  }
}
