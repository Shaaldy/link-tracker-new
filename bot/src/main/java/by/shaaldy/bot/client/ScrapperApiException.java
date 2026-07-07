package by.shaaldy.bot.client;

import org.springframework.http.HttpStatusCode;

import by.shaaldy.bot.dto.scrapper.ApiErrorResponse;
import lombok.Getter;

@Getter
public class ScrapperApiException extends RuntimeException {
  private final HttpStatusCode status;
  private final ApiErrorResponse error;

  public ScrapperApiException(HttpStatusCode status, ApiErrorResponse error) {
    super("scrapper responded " + status + ": " + error.getDescription());
    this.status = status;
    this.error = error;
  }

  public String userMessage() {
    return error.getDescription();
  }
}
