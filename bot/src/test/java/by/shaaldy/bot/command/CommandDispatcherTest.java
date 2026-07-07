package by.shaaldy.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommandDispatcherTest {

  @Mock Command start;

  private static final long CHAT = 1L;

  @Test
  void dispatch_knownCommand_executesIt() {
    when(start.command()).thenReturn("/start");
    when(start.execute(CHAT, "/start")).thenReturn("ok");
    CommandDispatcher dispatcher = new CommandDispatcher(List.of(start));

    assertThat(dispatcher.dispatch(CHAT, "/start")).isEqualTo("ok");
  }

  @Test
  void dispatch_commandWithArguments_passesFullTextAndRoutesByFirstToken() {
    when(start.command()).thenReturn("/track");
    when(start.execute(CHAT, "/track https://github.com/a/b")).thenReturn("tracking");
    CommandDispatcher dispatcher = new CommandDispatcher(List.of(start));

    // роутинг по первому токену, но в execute уходит весь текст
    assertThat(dispatcher.dispatch(CHAT, "/track https://github.com/a/b")).isEqualTo("tracking");
  }

  @Test
  void dispatch_unknownCommand_returnsFallbackAndDoesNotExecute() {
    when(start.command()).thenReturn("/start");
    CommandDispatcher dispatcher = new CommandDispatcher(List.of(start));

    String result = dispatcher.dispatch(CHAT, "/unknown");

    assertThat(result).isEqualTo("Неизвестная команда. Список команд: /help");
    verify(start, never()).execute(anyLong(), anyString());
  }

  @Test
  void all_returnsRegisteredCommands() {
    when(start.command()).thenReturn("/start");
    CommandDispatcher dispatcher = new CommandDispatcher(List.of(start));

    assertThat(dispatcher.all()).containsExactly(start);
  }
}
