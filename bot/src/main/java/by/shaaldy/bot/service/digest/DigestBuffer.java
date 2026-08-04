package by.shaaldy.bot.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DigestBuffer {

    private static final String KEY_PREFIX = "digest:";

    private final StringRedisTemplate redis;

    /** Добавляет текст уведомления в накопитель чата. */
    public void append(long chatId, String text) {
        redis.opsForList().rightPush(key(chatId), text);
    }

    /** Возвращает все накопленные уведомления чата, не удаляя их. */
    public List<String> get(long chatId) {
        List<String> values = redis.opsForList().range(key(chatId), 0, -1);
        return values == null ? List.of() : values;
    }

    /** Полностью очищает накопитель чата (после успешной отправки дайджеста). */
    public void clear(long chatId) {
        redis.delete(key(chatId));
    }

    private String key(long chatId) {
        return KEY_PREFIX + chatId;
    }
}