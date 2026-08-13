package by.shaaldy.bot.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;

import by.shaaldy.bot.command.CommandDispatcher;
import by.shaaldy.bot.dialog.DialogHandler;
import by.shaaldy.bot.dialog.DialogStateHolder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class UpdateListenerTest {

  @ParameterizedTest
  @ValueSource(ints = {5, 10, 15})
  void incrementsUserMessagesCounterForEachMessage(int messageCount) {
    TelegramBot telegramBot = mock(TelegramBot.class);
    MessageSender messageSender = mock(MessageSender.class);
    CommandDispatcher commandDispatcher = mock(CommandDispatcher.class);
    DialogStateHolder dialogStateHolder = new DialogStateHolder();
    DialogHandler dialogHandler = mock(DialogHandler.class);
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    when(commandDispatcher.dispatch(anyLong(), anyString())).thenReturn("ok");

    UpdateListener listener =
        new UpdateListener(
            telegramBot,
            messageSender,
            commandDispatcher,
            dialogStateHolder,
            dialogHandler,
            registry);

    for (int i = 0; i < messageCount; i++) {
      listener.handle(update(555L + i, "/help"));
    }

    assertThat(registry.get("bot.user.messages").counter().count()).isEqualTo(messageCount);
  }

  private Update update(long chatId, String text) {
    Chat chat = mock(Chat.class);
    when(chat.id()).thenReturn(chatId);

    Message message = mock(Message.class);
    when(message.text()).thenReturn(text);
    when(message.chat()).thenReturn(chat);

    Update update = mock(Update.class);
    when(update.message()).thenReturn(message);
    return update;
  }
}
