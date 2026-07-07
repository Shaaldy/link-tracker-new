package by.shaaldy.scrapper.domain;

import java.net.URI;
import java.util.List;

public record TrackedLink(long id, URI url, List<String> tags, List<String> filters) {
  public TrackedLink {
    tags = List.copyOf(tags);
    filters = List.copyOf(filters);
  }
}
