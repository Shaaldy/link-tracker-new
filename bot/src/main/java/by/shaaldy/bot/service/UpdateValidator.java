package by.shaaldy.bot.service;

import java.util.List;

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
    if (isEmpty(update.getInstantTgChatIds()) && isEmpty(update.getDigestTgChatIds())) {
      throw new InvalidUpdateException("tgChatIds is empty");
    }
  }

  private static boolean isEmpty(List<Long> ids) {
    return ids == null || ids.isEmpty();
  }
}
