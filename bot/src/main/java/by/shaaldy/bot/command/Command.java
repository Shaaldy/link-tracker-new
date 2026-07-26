package by.shaaldy.bot.command;

public interface Command {
  String command();

  String description();

  String execute(long chatIt, String text);

  default boolean requiresRegistration() {
    return true;
  }
}
