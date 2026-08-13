package by.shaaldy.scrapper.client;

import java.net.URI;
import java.time.Instant;

import by.shaaldy.scrapper.domain.UpdateDetails;

public interface UpdateChecker {
  boolean supports(URI url);

  Instant fetchLastActivity(URI url);

  UpdateDetails fetchDetails(URI url);

  String type();
}
