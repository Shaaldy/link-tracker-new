package by.shaaldy.scrapper.validation;

import java.net.URI;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import by.shaaldy.scrapper.exception.UnsupportedLinkException;

@Component
public class LinkValidator {

  private static final Pattern GITHUB_REPO = Pattern.compile("^/[^/]+/[^/]+(/.*)?$");
  private static final Pattern SO_QUESTION = Pattern.compile("^/(questions|q)/\\d+(/.*)?$");

  public void validate(URI url) {
    String host = url.getHost();
    String path = url.getPath();
    if (host == null || path == null || !isSupported(host, path)) {
      throw new UnsupportedLinkException(url);
    }
  }

  private boolean isSupported(String host, String path) {
    return isGitHubRepo(host, path) || isStackOverflowQuestion(host, path);
  }

  private boolean isGitHubRepo(String host, String path) {
    return host.equals("github.com") && GITHUB_REPO.matcher(path).matches();
  }

  private boolean isStackOverflowQuestion(String host, String path) {
    return host.equals("stackoverflow.com") && SO_QUESTION.matcher(path).matches();
  }
}