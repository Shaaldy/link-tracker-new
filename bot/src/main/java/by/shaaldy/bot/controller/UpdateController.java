package by.shaaldy.bot.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import by.shaaldy.bot.dto.bot.LinkUpdate;
import by.shaaldy.bot.notification.UpdateProcessor;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.message-transport", havingValue = "HTTP", matchIfMissing = true)
public class UpdateController {

  private final UpdateProcessor processor;

  @PostMapping("/updates")
  public ResponseEntity<Void> receiveUpdate(@RequestBody LinkUpdate update) {
    processor.process(update);
    return ResponseEntity.ok().build();
  }
}
