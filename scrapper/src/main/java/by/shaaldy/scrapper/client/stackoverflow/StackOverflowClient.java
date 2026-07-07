package by.shaaldy.scrapper.client.stackoverflow;

import java.net.URI;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import by.shaaldy.scrapper.client.UpdateChecker;
import by.shaaldy.scrapper.config.AppProperties;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StackOverflowClient implements UpdateChecker {

  private static final Pattern PATH = Pattern.compile("^/(?:questions|q)/(\\d+)(?:/.*)?$");

  private final StackOverflowApi api;
  private final AppProperties properties;

  @Override
  public boolean supports(URI url) {
    return "stackoverflow.com".equals(url.getHost()) && PATH.matcher(url.getPath()).matches();
  }

  @Override
  public Instant fetchLastActivity(URI url) {
    Matcher matcher = PATH.matcher(url.getPath());
    matcher.matches();
    long id = Long.parseLong(matcher.group(1));

    String key = properties.stackOverflow().key();
    StackOverflowResponse response =
        api.getQuestion(id, "stackoverflow", StringUtils.hasText(key) ? key : null);

    return Instant.ofEpochSecond(response.items().getFirst().lastActivityDate());
  }
}
