package by.shaaldy.bot.notification;

import by.shaaldy.bot.dto.bot.LinkUpdate;

public final class UpdateValidator {

  private UpdateValidator() {}

  public static void validate(LinkUpdate update) {
    if (update == null) {
      throw new InvalidUpdateException("update is null");
    }
    if (update.getUrl() == null) {
      throw new InvalidUpdateException("url is null");
    }
    if (update.getTgChatIds() == null || update.getTgChatIds().isEmpty()) {
      throw new InvalidUpdateException("tgChatIds is empty");
    }
  }
}
