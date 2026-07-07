package by.shaaldy.scrapper.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import by.shaaldy.scrapper.domain.TrackedLink;
import by.shaaldy.scrapper.dto.scrapper.AddLinkRequest;
import by.shaaldy.scrapper.dto.scrapper.LinkResponse;
import by.shaaldy.scrapper.dto.scrapper.ListLinksResponse;
import by.shaaldy.scrapper.dto.scrapper.RemoveLinkRequest;
import by.shaaldy.scrapper.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LinkController {

  private static final String CHAT_ID_HEADER = "Tg-Chat-Id";

  private final SubscriptionService service;

  @GetMapping("/links")
  public ResponseEntity<ListLinksResponse> getLinks(@RequestHeader(CHAT_ID_HEADER) long chatId) {
    List<LinkResponse> links =
        service.getLinks(chatId).stream().map(LinkController::toResponse).toList();
    ListLinksResponse body = new ListLinksResponse().links(links).size(links.size());
    return ResponseEntity.ok(body);
  }

  @PostMapping("/links")
  public ResponseEntity<LinkResponse> addLink(
      @RequestHeader(CHAT_ID_HEADER) long chatId, @RequestBody AddLinkRequest request) {
    TrackedLink added =
        service.addLink(chatId, request.getLink(), request.getTags(), request.getFilters());
    return ResponseEntity.ok(toResponse(added));
  }

  @DeleteMapping("/links")
  public ResponseEntity<LinkResponse> removeLink(
      @RequestHeader(CHAT_ID_HEADER) long chatId, @RequestBody RemoveLinkRequest request) {
    TrackedLink removed = service.removeLink(chatId, request.getLink());
    return ResponseEntity.ok(toResponse(removed));
  }

  private static LinkResponse toResponse(TrackedLink link) {
    return new LinkResponse()
        .id(link.id())
        .url(link.url())
        .tags(link.tags())
        .filters(link.filters());
  }
}
