package by.shaaldy.bot.command;

import by.shaaldy.bot.dialog.DialogState;
import by.shaaldy.bot.dialog.DialogStateHolder;
import by.shaaldy.bot.service.NotificationMode;
import by.shaaldy.bot.service.NotificationModeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModeCommand implements Command {

    private final DialogStateHolder holder;
    private final NotificationModeService modeService;

    @Override
    public String command() {
        return "/mode";
    }

    @Override
    public String description() {
        return "режим уведомлений: сразу или дайджест раз в сутки";
    }

    @Override
    public String execute(long chatId, String text) {
        String[] parts = text.strip().split("\\s+");

        if (parts.length == 1) {
            holder.get(chatId).setState(DialogState.AWAITING_MODE);
            return "Выберите режим уведомлений:\n1. instant — сразу\n2. digest — дайджест раз в сутки";
        }

        String mode = parts[1].toLowerCase();

        if (mode.equals("instant")) {
            return modeService.apply(chatId, NotificationMode.INSTANT, null);
        }

        if (mode.equals("digest")) {
            if (parts.length >= 3) {
                Integer hour = modeService.parseHour(parts[2]);
                if (hour == null) {
                    return "Час должен быть числом от 0 до 23.";
                }
                return modeService.apply(chatId, NotificationMode.DIGEST, hour);
            }
            holder.get(chatId).setState(DialogState.AWAITING_DIGEST_HOUR);
            return "Во сколько присылать дайджест? Введите час (0–23):";
        }

        return "Неизвестный режим. Используйте: /mode instant  или  /mode digest [час].";
    }
}