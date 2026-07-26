package by.shaaldy.scrapper.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import by.shaaldy.scrapper.domain.TrackedLink;
import by.shaaldy.scrapper.dto.scrapper.*;
import by.shaaldy.scrapper.exception.LinkNotFoundException;
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
  public ResponseEntity<ListLinksResponse> getLinks(
      @RequestHeader(CHAT_ID_HEADER) long chatId, @RequestParam(required = false) String tag) {
    List<TrackedLink> tracked =
        (tag == null || tag.isBlank())
            ? service.getLinks(chatId)
            : service.getLinksByTag(chatId, tag);
    List<LinkResponse> links = tracked.stream().map(LinkController::toResponse).toList();
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

  @GetMapping("/tags")
  public ResponseEntity<List<String>> getTags(@RequestHeader(CHAT_ID_HEADER) long chatId) {
    List<String> tags = List.copyOf(service.getTags(chatId));
    return ResponseEntity.ok(tags);
  }

  @PostMapping("/links/tags")
  public ResponseEntity<LinkResponse> addTag(
      @RequestHeader(CHAT_ID_HEADER) long chatId, @RequestBody TagRequest request) {
    service.addTag(chatId, request.getUrl(), request.getTag());
    return ResponseEntity.ok(currentSubscription(chatId, request.getUrl()));
  }

  @DeleteMapping("/links/tags")
  public ResponseEntity<LinkResponse> removeTag(
      @RequestHeader(CHAT_ID_HEADER) long chatId, @RequestBody TagRequest request) {
    service.removeTag(chatId, request.getUrl(), request.getTag());
    return ResponseEntity.ok(currentSubscription(chatId, request.getUrl()));
  }

  private static LinkResponse toResponse(TrackedLink link) {
    return new LinkResponse()
        .id(link.id())
        .url(link.url())
        .tags(link.tags())
        .filters(link.filters());
  }

  private LinkResponse currentSubscription(long chatId, java.net.URI url) {
    return service.getLinks(chatId).stream()
        .filter(link -> link.url().equals(url))
        .findFirst()
        .map(LinkController::toResponse)
        .orElseThrow(() -> new LinkNotFoundException(chatId, url));
  }
}
