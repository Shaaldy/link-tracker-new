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

  // Свежайший ответ: sort=activity desc, один элемент.
  @GetExchange("/questions/{id}/answers?filter=withbody&sort=activity&order=desc&pagesize=1")
  StackOverflowResponse getAnswers(
      @PathVariable long id, @RequestParam String site, @RequestParam(required = false) String key);

  // Свежайший комментарий: sort=creation desc (у comments нет activity), один элемент.
  @GetExchange("/questions/{id}/comments?filter=withbody&sort=creation&order=desc&pagesize=1")
  StackOverflowResponse getComments(
      @PathVariable long id, @RequestParam String site, @RequestParam(required = false) String key);
}
