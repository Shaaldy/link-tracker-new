package by.shaaldy.bot.dialog;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DialogContext {
  private DialogState state = DialogState.IDLE;
  private String link;
  private List<String> tags;
}
