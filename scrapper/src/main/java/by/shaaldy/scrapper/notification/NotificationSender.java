package by.shaaldy.scrapper.notification;

import by.shaaldy.scrapper.dto.bot.LinkUpdate;

public interface NotificationSender {

  void send(LinkUpdate update);
}
