package by.shaaldy.scrapper.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IpRateLimitingInterceptor implements HandlerInterceptor {

  private final RateLimiterRegistry rateLimiterRegistry;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String ip = request.getRemoteAddr();
    if (rateLimiterRegistry.rateLimiter(ip).acquirePermission()) {
      return true;
    }
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType("application/json");
    response.getWriter().write("{\"description\":\"Too many requests\"}");
    return false;
  }
}
