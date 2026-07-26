package by.shaaldy.scrapper.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import by.shaaldy.scrapper.dto.scrapper.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ScrapperEndToEndIT {

  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

  static {
    POSTGRES.start();
  }

  @Autowired TestRestTemplate rest;

  private static final String CHAT_HEADER = "Tg-Chat-Id";
  private static final long CHAT = 9001L;
  private static final URI LINK = URI.create("https://github.com/e2e/repo");

  private HttpHeaders chatHeader(long chatId) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(CHAT_HEADER, String.valueOf(chatId));
    return headers;
  }

  @Test
  void fullFlow_registerAddListRemove_worksEndToEnd() {
    // 1. Регистрация чата
    ResponseEntity<Void> reg = rest.postForEntity("/tg-chat/{id}", null, Void.class, CHAT);
    assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 2. Добавление ссылки с тегами через HTTP
    AddLinkRequest addBody =
        new AddLinkRequest().link(LINK).tags(List.of("работа")).filters(List.of());
    ResponseEntity<LinkResponse> add =
        rest.exchange(
            "/links",
            HttpMethod.POST,
            new HttpEntity<>(addBody, chatHeader(CHAT)),
            LinkResponse.class);
    assertThat(add.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(add.getBody().getUrl()).isEqualTo(LINK);
    assertThat(add.getBody().getTags()).containsExactly("работа");

    // 3. Список ссылок чата через HTTP
    ResponseEntity<ListLinksResponse> list =
        rest.exchange(
            "/links", HttpMethod.GET, new HttpEntity<>(chatHeader(CHAT)), ListLinksResponse.class);
    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody().getSize()).isEqualTo(1);
    assertThat(list.getBody().getLinks().getFirst().getUrl()).isEqualTo(LINK);

    // 4. Удаление ссылки через HTTP
    RemoveLinkRequest removeBody = new RemoveLinkRequest().link(LINK);
    ResponseEntity<LinkResponse> remove =
        rest.exchange(
            "/links",
            HttpMethod.DELETE,
            new HttpEntity<>(removeBody, chatHeader(CHAT)),
            LinkResponse.class);
    assertThat(remove.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 5. Список снова пуст
    ResponseEntity<ListLinksResponse> afterRemove =
        rest.exchange(
            "/links", HttpMethod.GET, new HttpEntity<>(chatHeader(CHAT)), ListLinksResponse.class);
    assertThat(afterRemove.getBody().getSize()).isEqualTo(0);

    // 6. Удаление чата (каскад)
    ResponseEntity<Void> del =
        rest.exchange("/tg-chat/{id}", HttpMethod.DELETE, HttpEntity.EMPTY, Void.class, CHAT);
    assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void addLink_duplicateChat_returnsConflict() {
    long chat = 9002L;
    rest.postForEntity("/tg-chat/{id}", null, Void.class, chat);

    // повторная регистрация → 409 (если настроен @RestControllerAdvice для ChatAlreadyExists)
    ResponseEntity<Void> dup = rest.postForEntity("/tg-chat/{id}", null, Void.class, chat);
    assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void getLinks_unregisteredChat_returnsNotFound() {
    ResponseEntity<ListLinksResponse> resp =
        rest.exchange(
            "/links", HttpMethod.GET, new HttpEntity<>(chatHeader(9999L)), ListLinksResponse.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void tags_fullFlow_filterListAddRemove() {
    long chat = 9100L;
    URI urlA = URI.create("https://github.com/tags/a");
    URI urlB = URI.create("https://github.com/tags/b");
    rest.postForEntity("/tg-chat/{id}", null, Void.class, chat);

    // подписка A с тегом "работа", B с тегом "хобби"
    addLink(chat, urlA, List.of("работа"));
    addLink(chat, urlB, List.of("хобби"));

    // 1. Фильтрация: GET /links?tag=работа → только A
    ResponseEntity<ListLinksResponse> filtered =
        rest.exchange(
            "/links?tag=работа",
            HttpMethod.GET,
            new HttpEntity<>(chatHeader(chat)),
            ListLinksResponse.class);
    assertThat(filtered.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(filtered.getBody().getSize()).isEqualTo(1);
    assertThat(filtered.getBody().getLinks().getFirst().getUrl()).isEqualTo(urlA);

    // 2. Список тегов: GET /tags → работа, хобби
    ResponseEntity<List> tags =
        rest.exchange("/tags", HttpMethod.GET, new HttpEntity<>(chatHeader(chat)), List.class);
    assertThat(tags.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(tags.getBody()).containsExactlyInAnyOrder("работа", "хобби");

    // 3. Добавить тег к A: POST /links/tags
    TagRequest addReq = new TagRequest().url(urlA).tag("срочно");
    ResponseEntity<LinkResponse> added =
        rest.exchange(
            "/links/tags",
            HttpMethod.POST,
            new HttpEntity<>(addReq, chatHeader(chat)),
            LinkResponse.class);
    assertThat(added.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(added.getBody().getTags()).containsExactlyInAnyOrder("работа", "срочно");

    // 4. Убрать тег у A: DELETE /links/tags
    TagRequest removeReq = new TagRequest().url(urlA).tag("работа");
    ResponseEntity<LinkResponse> removed =
        rest.exchange(
            "/links/tags",
            HttpMethod.DELETE,
            new HttpEntity<>(removeReq, chatHeader(chat)),
            LinkResponse.class);
    assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(removed.getBody().getTags()).containsExactly("срочно"); // "работа" убран
  }

  @Test
  void getTags_unregisteredChat_returnsNotFound() {
    ResponseEntity<ApiErrorResponse> resp =
        rest.exchange(
            "/tags", HttpMethod.GET, new HttpEntity<>(chatHeader(9199L)), ApiErrorResponse.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(resp.getBody().getExceptionName()).isEqualTo("ChatNotFoundException");
  }

  // helper: добавить ссылку с тегами через HTTP
  private void addLink(long chat, URI url, List<String> tags) {
    AddLinkRequest body = new AddLinkRequest().link(url).tags(tags).filters(List.of());
    rest.exchange(
        "/links", HttpMethod.POST, new HttpEntity<>(body, chatHeader(chat)), LinkResponse.class);
  }
}
