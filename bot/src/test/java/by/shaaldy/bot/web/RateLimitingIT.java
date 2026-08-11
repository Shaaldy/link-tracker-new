package by.shaaldy.bot.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import by.shaaldy.bot.dto.bot.LinkUpdate;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "app.rate-limiter.limit-for-period=3",
      "app.rate-limiter.limit-refresh-period=60s",
      "app.rate-limiter.timeout-duration=0s",
      "app.message-transport=HTTP"
    })
class RateLimitingIT {

  @LocalServerPort private int port;

  @Test
  void repeatedRequests_exceedingLimit_return429() {
    TestRestTemplate restTemplate = new TestRestTemplate();
    String url = "http://localhost:" + port + "/updates";
    LinkUpdate update = new LinkUpdate().id(1L);

    for (int i = 0; i < 3; i++) {
      assertThat(restTemplate.postForEntity(url, update, Void.class).getStatusCode())
          .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    assertThat(restTemplate.postForEntity(url, update, Void.class).getStatusCode())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }
}
