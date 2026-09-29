package by.shaaldy.bot.telegram;

public interface MessageHandler {
    String handle(long chatId, String text);
    boolean supports(long chatId);
}
