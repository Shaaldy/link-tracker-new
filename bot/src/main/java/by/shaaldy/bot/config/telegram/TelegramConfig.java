package by.shaaldy.bot.config.telegram;

import by.shaaldy.bot.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pengrad.telegrambot.TelegramBot;

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
