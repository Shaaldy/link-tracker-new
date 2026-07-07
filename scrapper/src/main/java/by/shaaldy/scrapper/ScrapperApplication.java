package by.shaaldy.scrapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import by.shaaldy.scrapper.config.AppProperties;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ScrapperApplication {

  public static void main(String[] args) {
    SpringApplication.run(ScrapperApplication.class, args);
  }
}
