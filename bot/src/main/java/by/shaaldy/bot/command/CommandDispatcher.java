package by.shaaldy.bot.command;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CommandDispatcher {
  private final Map<String, Command> commands;

  public CommandDispatcher(List<Command> commandList) {
    this.commands =
        commandList.stream().collect(Collectors.toMap(Command::command, Function.identity()));
  }

  public String dispatch(long chatId, String text) {
    String commandKey = text.split("\\s+")[0];
    Optional<Command> command = Optional.ofNullable(commands.get(commandKey));

    command.ifPresentOrElse(
        c -> log.info("Dispatching command {} for chat {}", commandKey, chatId),
        () -> log.warn("Unknown command {} from chat {}", commandKey, chatId));

    return command
        .map(c -> c.execute(chatId, text))
        .orElse("Неизвестная команда. Список команд: /help");
  }

  public List<Command> all() {
    return List.copyOf(commands.values());
  }
}
