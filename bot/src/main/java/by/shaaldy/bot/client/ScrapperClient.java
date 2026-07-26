package by.shaaldy.bot.client;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import by.shaaldy.bot.dto.scrapper.*;

@HttpExchange
public interface ScrapperClient {

  String CHAT_ID_HEADER = "Tg-Chat-Id";

  @PostExchange("/tg-chat/{id}")
  void registerChat(@PathVariable long id);

  @DeleteExchange("/tg-chat/{id}")
  void deleteChat(@PathVariable long id);

  @GetExchange("/tg-chat/{id}")
  Boolean existChat(@PathVariable long id);

  @GetExchange("/links")
  ListLinksResponse listLinks(@RequestHeader(CHAT_ID_HEADER) long chatId);

  @PostExchange("/links")
  LinkResponse addLink(
      @RequestHeader(CHAT_ID_HEADER) long chatId, @RequestBody AddLinkRequest request);

  @DeleteExchange("/links")
  LinkResponse removeLink(
      @RequestHeader(CHAT_ID_HEADER) long chatId, @RequestBody RemoveLinkRequest request);

  @GetExchange("/links")
  ListLinksResponse listLinksByTag(
      @RequestHeader(CHAT_ID_HEADER) long chatId, @RequestParam String tag);

  @GetExchange("/tags")
  List<String> listTags(@RequestHeader(CHAT_ID_HEADER) long chatId);

  @PostExchange("/links/tags")
  LinkResponse addTag(@RequestHeader(CHAT_ID_HEADER) long chatId, @RequestBody TagRequest request);

  @DeleteExchange("/links/tags")
  LinkResponse removeTag(
      @RequestHeader(CHAT_ID_HEADER) long chatId, @RequestBody TagRequest request);
}
