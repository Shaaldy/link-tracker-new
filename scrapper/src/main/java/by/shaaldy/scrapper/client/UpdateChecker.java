package by.shaaldy.scrapper.client;

import java.net.URI;
import java.time.Instant;

public interface UpdateChecker {
  boolean supports(URI url);

  Instant fetchLastActivity(URI url);
}
