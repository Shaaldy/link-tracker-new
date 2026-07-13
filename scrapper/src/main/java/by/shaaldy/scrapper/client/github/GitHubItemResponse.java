package by.shaaldy.scrapper.client.github;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Элемент PR или Issue из GitHub API (эндпоинты /pulls, /issues). Поля детализации: title, автор
 * (user.login), время создания, body (описание).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubItemResponse(
    String title,
    User user,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("updated_at") Instant updatedAt,
    String body) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record User(String login) {}
}
