package by.shaaldy.scrapper.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import liquibase.integration.spring.SpringLiquibase;

@Configuration
public class LiquibaseConfig {

  @Bean
  public SpringLiquibase liquibase(
      DataSource dataSource,
      @Value("${app.migrations.changelog:migrations/master.xml}") String changelog) {

    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog("classpath:" + changelog);
    return liquibase;
  }
}
