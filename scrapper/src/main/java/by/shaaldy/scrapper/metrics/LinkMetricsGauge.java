package by.shaaldy.scrapper.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import by.shaaldy.scrapper.repository.LinkMetricsRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LinkMetricsGauge {

  private final LinkMetricsRepository metricsRepository;
  private final MeterRegistry registry;
  private final Map<String, AtomicLong> countsByType = new ConcurrentHashMap<>();

  public LinkMetricsGauge(LinkMetricsRepository metricsRepository, MeterRegistry registry) {
    this.metricsRepository = metricsRepository;
    this.registry = registry;
  }

  @Scheduled(fixedDelayString = "${app.metrics.links-refresh-interval}")
  public void refresh() {
    Map<String, Long> counts = metricsRepository.countActiveByType();
    counts.forEach((type, count) -> holderFor(type).set(count));
  }

  private AtomicLong holderFor(String type) {
    return countsByType.computeIfAbsent(
        type, t -> registry.gauge("scrapper.links.active", Tags.of("type", t), new AtomicLong(0)));
  }
}
