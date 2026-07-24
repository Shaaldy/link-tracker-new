package by.shaaldy.bot.command;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import by.shaaldy.bot.service.RegistrationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CommandDispatcher {
  private final Map<String, Command> commands;
  private final RegistrationService registrationService;

  @Autowired
  public CommandDispatcher(List<Command> commandList, RegistrationService registrationService) {
    this.commands =
        commandList.stream().collect(Collectors.toMap(Command::command, Function.identity()));
    this.registrationService = registrationService;
  }

  public String dispatch(long chatId, String text) {
    String commandKey = text.split("\\s+")[0];
    Command command = commands.get(commandKey);

    if (command == null) {
      log.warn("Unknown command {} from chat {}", commandKey, chatId);
      return "Неизвестная команда. Список команд: /help";
    }

    if (command.requiresRegistration() && !registrationService.isRegistered(chatId)) {
      return "Вы не зарегистрированы. Отправьте /start, чтобы начать.";
    }

    log.info("Dispatching command {} for chat {}", commandKey, chatId);
    return command.execute(chatId, text);
  }

  public List<Command> all() {
    return List.copyOf(commands.values());
  }
}
