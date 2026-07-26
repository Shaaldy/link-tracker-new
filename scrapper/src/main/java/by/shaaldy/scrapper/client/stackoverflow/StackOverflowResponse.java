package by.shaaldy.scrapper.client.stackoverflow;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StackOverflowResponse(List<Item> items) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Item(
      @JsonProperty("last_activity_date") long lastActivityDate,
      @JsonProperty("creation_date") long creationDate,
      String title,
      Owner owner,
      String body) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(@JsonProperty("display_name") String displayName) {}
  }
}
