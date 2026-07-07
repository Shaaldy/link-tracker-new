package by.shaaldy.scrapper.client.github;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepoResponse(
    @JsonProperty("pushed_at") Instant pushedAt, @JsonProperty("updated_at") Instant updatedAt) {}
