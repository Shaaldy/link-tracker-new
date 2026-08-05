package by.shaaldy.scrapper.controller;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import by.shaaldy.scrapper.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationController {

  private final SubscriptionService service;

  @GetMapping("/notifications/digest-recipients")
  public ResponseEntity<Set<Long>> digestRecipients(@RequestParam int hour) {
    return ResponseEntity.ok(service.findDigestRecipients(hour));
  }
}
