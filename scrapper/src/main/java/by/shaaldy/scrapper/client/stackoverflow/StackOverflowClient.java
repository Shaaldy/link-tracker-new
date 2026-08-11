package by.shaaldy.scrapper.client.stackoverflow;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import by.shaaldy.scrapper.client.UpdateChecker;
import by.shaaldy.scrapper.config.AppProperties;
import by.shaaldy.scrapper.domain.UpdateDetails;
import by.shaaldy.scrapper.util.TextPreview;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StackOverflowClient implements UpdateChecker {

  private static final Pattern PATH = Pattern.compile("^/(?:questions|q)/(\\d+)(?:/.*)?$");

  private final StackOverflowApi api;
  private final AppProperties properties;
  private final RetryRegistry retryRegistry;
  private final CircuitBreakerRegistry circuitBreakerRegistry;

  @Override
  public boolean supports(URI url) {
    return "stackoverflow.com".equals(url.getHost()) && PATH.matcher(url.getPath()).matches();
  }

  @Override
  public Instant fetchLastActivity(URI url) {
    long id = extractId(url);
    StackOverflowResponse.Item question = firstItem(getQuestion(id, key()));
    return question == null ? Instant.EPOCH : Instant.ofEpochSecond(question.lastActivityDate());
  }

  @Override
  public UpdateDetails fetchDetails(URI url) {
    long id = extractId(url);
    String k = key();

    // Тема вопроса — общая для всех типов обновления (ДЗ: "текст темы вопроса").
    StackOverflowResponse.Item question = firstItem(getQuestion(id, k));
    String title = question == null ? null : question.title();

    // Три кандидата: свежайший ответ, свежайший комментарий, само событие вопроса (правка).
    // Выбираем самый свежий по времени. У ответа/коммента author/preview свои, title — вопроса.
    List<Candidate> candidates = new ArrayList<>();
    if (question != null) {
      candidates.add(
          new Candidate(
              question.lastActivityDate(),
              new UpdateDetails(
                  title,
                  ownerName(question),
                  Instant.ofEpochSecond(question.creationDate()),
                  TextPreview.preview(question.body()))));
    }
    addBodyCandidate(candidates, firstItem(getAnswers(id, k)), title);
    addBodyCandidate(candidates, firstItem(getComments(id, k)), title);

    return candidates.stream()
        .max(Comparator.comparingLong(Candidate::at))
        .map(Candidate::details)
        .orElseGet(() -> new UpdateDetails(title, null, null, null));
  }

  private StackOverflowResponse getQuestion(long id, String key) {
    return decorate("stackoverflow-getQuestion", () -> api.getQuestion(id, "stackoverflow", key));
  }

  private StackOverflowResponse getAnswers(long id, String key) {
    return decorate("stackoverflow-getAnswers", () -> api.getAnswers(id, "stackoverflow", key));
  }

  private StackOverflowResponse getComments(long id, String key) {
    return decorate("stackoverflow-getComments", () -> api.getComments(id, "stackoverflow", key));
  }

  private <T> T decorate(String name, Supplier<T> call) {
    return circuitBreakerRegistry
        .circuitBreaker(name)
        .executeSupplier(() -> retryRegistry.retry(name).executeSupplier(call));
  }

  private static void addBodyCandidate(
      List<Candidate> candidates, StackOverflowResponse.Item item, String title) {
    if (item == null) {
      return;
    }
    candidates.add(
        new Candidate(
            item.lastActivityDate() > 0 ? item.lastActivityDate() : item.creationDate(),
            new UpdateDetails(
                title,
                ownerName(item),
                Instant.ofEpochSecond(item.creationDate()),
                TextPreview.preview(item.body()))));
  }

  private static String ownerName(StackOverflowResponse.Item item) {
    return item.owner() == null ? null : item.owner().displayName();
  }

  private long extractId(URI url) {
    Matcher matcher = PATH.matcher(url.getPath());
    matcher.matches();
    return Long.parseLong(matcher.group(1));
  }

  private String key() {
    String key = properties.stackoverflow().key();
    return StringUtils.hasText(key) ? key : null;
  }

  private static StackOverflowResponse.Item firstItem(StackOverflowResponse response) {
    return response == null || response.items() == null || response.items().isEmpty()
        ? null
        : response.items().getFirst();
  }

  /** Кандидат детализации с временем события (epoch seconds) — для выбора самого свежего. */
  private record Candidate(long at, UpdateDetails details) {}
}
