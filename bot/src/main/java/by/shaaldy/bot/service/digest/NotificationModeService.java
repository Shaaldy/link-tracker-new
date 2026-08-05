package by.shaaldy.bot.service.digest;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import by.shaaldy.bot.exception.ScrapperApiException;
import by.shaaldy.bot.client.ScrapperClient;
import by.shaaldy.bot.dto.scrapper.NotificationModeRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationModeService {

  private final ScrapperClient scrapperClient;

  public String apply(long chatId, NotificationMode mode, Integer digestHour) {
    NotificationModeRequest request =
        new NotificationModeRequest().mode(toApiMode(mode)).digestHour(digestHour);
    try {
      scrapperClient.updateNotificationMode(chatId, request);
      return mode == NotificationMode.DIGEST
          ? "Режим уведомлений: дайджест раз в сутки в " + digestHour + ":00."
          : "Режим уведомлений: сразу при обнаружении.";
    } catch (ScrapperApiException e) {
      if (e.getStatus().value() == 404) {
        return "Сначала зарегистрируйтесь: /start.";
      }
      return e.userMessage();
    } catch (RestClientException e) {
      return "Сервис временно недоступен, попробуйте позже.";
    }
  }

  public Integer parseHour(String text) {
    try {
      int h = Integer.parseInt(text.strip());
      return (h >= 0 && h <= 23) ? h : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private NotificationModeRequest.ModeEnum toApiMode(NotificationMode mode) {
    return mode == NotificationMode.DIGEST
        ? NotificationModeRequest.ModeEnum.DIGEST
        : NotificationModeRequest.ModeEnum.INSTANT;
  }
}
