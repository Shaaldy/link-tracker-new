package by.shaaldy.scrapper.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import by.shaaldy.scrapper.domain.NotificationMode;
import by.shaaldy.scrapper.dto.scrapper.NotificationModeRequest;
import by.shaaldy.scrapper.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TgChatController {

  private final SubscriptionService service;

  @PostMapping("/tg-chat/{id}")
  public ResponseEntity<Void> registerChat(@PathVariable long id) {
    service.registerChat(id);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/tg-chat/{id}")
  public ResponseEntity<Void> deleteChat(@PathVariable long id) {
    service.removeChat(id);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/tg-chat/{id}")
  public ResponseEntity<Boolean> existChat(@PathVariable long id) {
    return ResponseEntity.ok(service.existChat(id));
  }

  @PutMapping("/tg-chat/{id}/notification-mode")
  public ResponseEntity<Void> updateNotificationMode(
      @PathVariable long id, @RequestBody NotificationModeRequest request) {
    NotificationMode mode = NotificationMode.valueOf(request.getMode().name());
    service.updateNotificationMode(id, mode, request.getDigestHour());
    return ResponseEntity.ok().build();
  }
}
