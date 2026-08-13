package by.shaaldy.scrapper.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import by.shaaldy.scrapper.repository.LinkMetricsRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class LinkMetricsGaugeTest {

  @Test
  void registersGaugePerTypeWithCurrentValue() {
    LinkMetricsRepository repository = mock(LinkMetricsRepository.class);
    when(repository.countActiveByType()).thenReturn(Map.of("github", 5L, "stackoverflow", 3L));
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    LinkMetricsGauge gauge = new LinkMetricsGauge(repository, registry);

    gauge.refresh();

    assertThat(registry.get("scrapper.links.active").tag("type", "github").gauge().value())
        .isEqualTo(5.0);
    assertThat(registry.get("scrapper.links.active").tag("type", "stackoverflow").gauge().value())
        .isEqualTo(3.0);
  }

  @Test
  void updatesExistingGaugeInsteadOfDuplicatingOnSecondRefresh() {
    LinkMetricsRepository repository = mock(LinkMetricsRepository.class);
    when(repository.countActiveByType())
        .thenReturn(Map.of("github", 5L))
        .thenReturn(Map.of("github", 8L));
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    LinkMetricsGauge gauge = new LinkMetricsGauge(repository, registry);

    gauge.refresh();
    gauge.refresh();

    assertThat(registry.get("scrapper.links.active").tag("type", "github").gauge().value())
        .isEqualTo(8.0);
    assertThat(registry.getMeters()).hasSize(1); // не задублировалась регистрация
  }

  @Test
  void handlesTypeDisappearingBetweenRefreshesByKeepingLastValue() {
    LinkMetricsRepository repository = mock(LinkMetricsRepository.class);
    when(repository.countActiveByType())
        .thenReturn(Map.of("github", 5L, "stackoverflow", 2L))
        .thenReturn(Map.of("github", 5L)); // stackoverflow больше не пришёл
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    LinkMetricsGauge gauge = new LinkMetricsGauge(repository, registry);

    gauge.refresh();
    gauge.refresh();

    // gauge для stackoverflow остаётся зарегистрированным со старым значением —
    // задокументированное поведение, не баг (см. обсуждение в теории)
    assertThat(registry.get("scrapper.links.active").tag("type", "stackoverflow").gauge().value())
        .isEqualTo(2.0);
  }
}
