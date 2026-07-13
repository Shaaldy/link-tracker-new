package by.shaaldy.scrapper.client.stackoverflow;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface StackOverflowApi {

  // filter=withbody — иначе API не возвращает title/body, только метаданные.
  @GetExchange("/questions/{id}?filter=withbody")
  StackOverflowResponse getQuestion(
      @PathVariable long id, @RequestParam String site, @RequestParam(required = false) String key);
}
