package by.shaaldy.scrapper.domain;

import java.net.URI;
import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class Link {

  private final long id;
  private final URI url;
  private final Instant createdAt;

  public Link(long id, URI url, Instant createdAt) {
    this.id = id;
    this.url = url;
    this.createdAt = createdAt;
  }
}
