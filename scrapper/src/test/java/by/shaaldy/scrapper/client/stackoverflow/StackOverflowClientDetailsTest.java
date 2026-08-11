package by.shaaldy.scrapper.client.stackoverflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import by.shaaldy.scrapper.config.AppProperties;
import by.shaaldy.scrapper.domain.UpdateDetails;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;

@ExtendWith(MockitoExtension.class)
class StackOverflowClientDetailsTest {

  @Mock StackOverflowApi api;
  @Mock AppProperties properties;
  StackOverflowClient client;

  private static final URI URL = URI.create("https://stackoverflow.com/questions/12345");
  private static final long OLD = 1_700_000_000L; // раньше
  private static final long MID = 1_710_000_000L;
  private static final long NEW = 1_720_000_000L; // свежее

  @BeforeEach
  void stubKey() {
    AppProperties.StackOverflow so = org.mockito.Mockito.mock(AppProperties.StackOverflow.class);
    lenient().when(so.key()).thenReturn(null);
    lenient().when(properties.stackoverflow()).thenReturn(so);
    client =
        new StackOverflowClient(
            api, properties, RetryRegistry.ofDefaults(), CircuitBreakerRegistry.ofDefaults());
  }

  private StackOverflowResponse question(long lastActivity) {
    return new StackOverflowResponse(
        List.of(
            new StackOverflowResponse.Item(
                lastActivity, OLD, "Как сделать X?", owner("questioner"), "тело вопроса")));
  }

  private StackOverflowResponse body(long activity, String author, String text) {
    return new StackOverflowResponse(
        List.of(new StackOverflowResponse.Item(activity, activity, null, owner(author), text)));
  }

  private StackOverflowResponse.Item.Owner owner(String name) {
    return new StackOverflowResponse.Item.Owner(name);
  }

  @Test
  void fetchDetails_answerIsFreshest_returnsAnswerWithQuestionTitle() {
    when(api.getQuestion(12345L, "stackoverflow", null)).thenReturn(question(OLD));
    when(api.getAnswers(12345L, "stackoverflow", null))
        .thenReturn(body(NEW, "answerer", "текст ответа"));
    when(api.getComments(12345L, "stackoverflow", null))
        .thenReturn(body(MID, "commenter", "коммент"));

    UpdateDetails d = client.fetchDetails(URL);

    assertThat(d.title()).isEqualTo("Как сделать X?"); // тема вопроса, общая
    assertThat(d.author()).isEqualTo("answerer");
    assertThat(d.preview()).isEqualTo("текст ответа");
  }

  @Test
  void fetchDetails_commentIsFreshest_returnsCommentWithQuestionTitle() {
    when(api.getQuestion(12345L, "stackoverflow", null)).thenReturn(question(OLD));
    when(api.getAnswers(12345L, "stackoverflow", null))
        .thenReturn(body(OLD, "answerer", "старый ответ"));
    when(api.getComments(12345L, "stackoverflow", null))
        .thenReturn(body(NEW, "commenter", "свежий коммент"));

    UpdateDetails d = client.fetchDetails(URL);

    assertThat(d.title()).isEqualTo("Как сделать X?");
    assertThat(d.author()).isEqualTo("commenter");
    assertThat(d.preview()).isEqualTo("свежий коммент");
  }

  @Test
  void fetchDetails_questionEditIsFreshest_returnsQuestionDetails() {
    // Вопрос отредактирован свежее ответов/комментариев (или их нет).
    when(api.getQuestion(12345L, "stackoverflow", null)).thenReturn(question(NEW));
    when(api.getAnswers(12345L, "stackoverflow", null))
        .thenReturn(body(OLD, "answerer", "старый ответ"));
    when(api.getComments(12345L, "stackoverflow", null))
        .thenReturn(body(MID, "commenter", "коммент"));

    UpdateDetails d = client.fetchDetails(URL);

    assertThat(d.title()).isEqualTo("Как сделать X?");
    assertThat(d.preview()).isEqualTo("тело вопроса");
  }
}
