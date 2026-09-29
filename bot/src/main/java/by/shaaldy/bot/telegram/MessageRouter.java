package by.shaaldy.bot.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageRouter {
    private final List<MessageHandler> handlers;

    public String route(long chatId, String text){
        return  handlers.stream()
                .filter(h -> h.supports(chatId))
                .findFirst()
                .map(h -> h.handle(chatId, text))
                .orElseThrow();
    }
}
