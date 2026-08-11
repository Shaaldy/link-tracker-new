package by.shaaldy.bot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import by.shaaldy.bot.dto.bot.LinkUpdate;
import by.shaaldy.bot.service.UpdateProcessor;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UpdateController {

  private final UpdateProcessor processor;

  @PostMapping("/updates")
  public ResponseEntity<Void> receiveUpdate(@RequestBody LinkUpdate update) {
    processor.process(update);
    return ResponseEntity.ok().build();
  }
}
