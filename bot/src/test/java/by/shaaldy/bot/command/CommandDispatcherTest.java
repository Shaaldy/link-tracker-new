package by.shaaldy.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import by.shaaldy.bot.service.RegistrationService;

@ExtendWith(MockitoExtension.class)
class CommandDispatcherTest {

  @Mock Command command;
  @Mock RegistrationService registrationService;

  private static final long CHAT = 1L;

  private CommandDispatcher dispatcher() {
    return new CommandDispatcher(List.of(command), registrationService);
  }

  @Test
  void dispatch_registeredChat_executesCommand() {
    when(command.command()).thenReturn("/track");
    when(command.requiresRegistration()).thenReturn(true);
    when(command.execute(CHAT, "/track")).thenReturn("ok");
    when(registrationService.isRegistered(CHAT)).thenReturn(true);

    assertThat(dispatcher().dispatch(CHAT, "/track")).isEqualTo("ok");
  }

  @Test
  void dispatch_unregisteredChat_blocksCommandAndAsksToStart() {
    when(command.command()).thenReturn("/track");
    when(command.requiresRegistration()).thenReturn(true);
    when(registrationService.isRegistered(CHAT)).thenReturn(false);

    String result = dispatcher().dispatch(CHAT, "/track");

    assertThat(result).contains("/start");
    verify(command, never()).execute(anyLong(), anyString());
  }

  @Test
  void dispatch_publicCommand_executesWithoutRegistrationCheck() {
    // /start и /help (requiresRegistration=false) выполняются без обращения к RegistrationService
    when(command.command()).thenReturn("/start");
    when(command.requiresRegistration()).thenReturn(false);
    when(command.execute(CHAT, "/start")).thenReturn("registered");

    assertThat(dispatcher().dispatch(CHAT, "/start")).isEqualTo("registered");
    verify(registrationService, never()).isRegistered(anyLong());
  }

  @Test
  void dispatch_commandWithArguments_routesByFirstTokenPassesFullText() {
    when(command.command()).thenReturn("/track");
    when(command.requiresRegistration()).thenReturn(true);
    when(registrationService.isRegistered(CHAT)).thenReturn(true);
    when(command.execute(CHAT, "/track https://github.com/a/b")).thenReturn("tracking");

    assertThat(dispatcher().dispatch(CHAT, "/track https://github.com/a/b")).isEqualTo("tracking");
  }

  @Test
  void dispatch_unknownCommand_returnsFallbackAndDoesNotExecute() {
    when(command.command()).thenReturn("/start");
    CommandDispatcher dispatcher = dispatcher();

    String result = dispatcher.dispatch(CHAT, "/unknown");

    assertThat(result).isEqualTo("Неизвестная команда. Список команд: /help");
    verify(command, never()).execute(anyLong(), anyString());
  }

  @Test
  void all_returnsRegisteredCommands() {
    when(command.command()).thenReturn("/start");

    assertThat(dispatcher().all()).containsExactly(command);
  }
}
