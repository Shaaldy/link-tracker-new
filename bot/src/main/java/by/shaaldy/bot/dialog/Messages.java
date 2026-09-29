package by.shaaldy.bot.dialog;

public final class Messages {
  private Messages() {}

  public static final String DIALOG_INACTIVE = "Диалог не активен. Наберите /help.";
  public static final String SERVICE_UNAVAILABLE = "Сервис временно недоступен, попробуйте позже.";
  public static final String INVALID_LINK =
      "Некорректная ссылка. Пример: https://github.com/owner/repo";
  public static final String LINK_NOT_TRACKED = "Эта ссылка не отслеживается.";
  public static final String CHAT_NOT_FOUND = "Ссылка или чат не найдены.";
}
