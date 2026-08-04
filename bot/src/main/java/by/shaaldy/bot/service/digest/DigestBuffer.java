package by.shaaldy.bot.service.digest;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Буфер накопленных уведомлений для DIGEST-режима (бонус: батчинг). НЕ кэш — это очередь состояния,
 * которую пользователь явно ждёт получить целиком при флаше. Использует Redis LIST напрямую через
 * StringRedisTemplate, а не Spring Cache abstraction, потому что паттерн доступа —
 * append/read-all/clear, а не read-through одного значения. Ключи: digest:{chatId}.
 */
@Component
@RequiredArgsConstructor
public class DigestBuffer {

  private static final String KEY_PREFIX = "digest:";

  private final StringRedisTemplate redis;

  public void append(long chatId, String text) {
    redis.opsForList().rightPush(key(chatId), text);
  }

  public List<String> get(long chatId) {
    List<String> values = redis.opsForList().range(key(chatId), 0, -1);
    return values == null ? List.of() : values;
  }

  public void clear(long chatId) {
    redis.delete(key(chatId));
  }

  private String key(long chatId) {
    return KEY_PREFIX + chatId;
  }
}
