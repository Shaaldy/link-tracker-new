package by.shaaldy.scrapper.repository;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
abstract class AbstractPostgresIT {

  @Autowired JdbcTemplate jdbcTemplate;

  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

  static {
    POSTGRES.start();
  }

  @BeforeEach
  void clean() {
    jdbcTemplate.execute(
        """
        DROP TABLE IF EXISTS link_filters;
        DROP TABLE IF EXISTS link_tags;
        DROP TABLE IF EXISTS chat_links;
        DROP TABLE IF EXISTS links;
        DROP TABLE IF EXISTS chats;
        """);
  }
}
