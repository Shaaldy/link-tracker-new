package by.shaaldy.bot.command;

import java.util.Comparator;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand implements Command {

  private final CommandDispatcher dispatcher;

  public HelpCommand(@Lazy CommandDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  @Override
  public String command() {
    return "/help";
  }

  @Override
  public String description() {
    return "список доступных команд";
  }

  @Override
  public String execute(long chatIt, String text) {
    return dispatcher.all().stream()
        .sorted(Comparator.comparing(Command::command))
        .map(c -> c.command() + " - " + c.description())
        .collect(Collectors.joining("\n"));
  }
}
