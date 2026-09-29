package by.shaaldy.bot.dialog;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import by.shaaldy.bot.dialog.step.DialogStep;

@Component
public class DialogHandler {
  private final DialogStateHolder holder;
  private final Map<DialogState, DialogStep> steps;

  public DialogHandler(List<DialogStep> steps, DialogStateHolder holder) {
    this.holder = holder;
    this.steps =
        steps.stream()
            .collect(
                Collectors.toMap(
                    DialogStep::state,
                    Function.identity(),
                    (a, b) -> {
                      throw new IllegalStateException("Дублирующийся шаг: " + a.state());
                    },
                    () -> new EnumMap<>(DialogState.class)));
  }

  public String handle(long chatId, String text) {
    DialogContext ctx = holder.get(chatId);
    DialogStep step = steps.get(ctx.getState());
    return step == null ? Messages.DIALOG_INACTIVE : step.handle(chatId, ctx, text);
  }
}
