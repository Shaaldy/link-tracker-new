package by.shaaldy.bot.config.telegram;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pengrad.telegrambot.TelegramBot;

import by.shaaldy.bot.config.AppProperties;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class TelegramConfig {
  private final AppProperties appProperties;

  @Bean
  public TelegramBot telegramBot() {
    return new TelegramBot(appProperties.telegramToken());
  }
}
