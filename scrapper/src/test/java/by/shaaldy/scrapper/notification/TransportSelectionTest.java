package by.shaaldy.scrapper.notification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import by.shaaldy.scrapper.config.AppProperties;
import by.shaaldy.scrapper.dto.bot.LinkUpdate;

@ExtendWith(MockitoExtension.class)
class TransportSelectionTest {

  @Mock private HttpNotificationSender httpSender;
  @Mock private KafkaNotificationSender kafkaSender;

  private static final LinkUpdate UPDATE = new LinkUpdate().id(1L);

  @Test
  void httpTransport_primarySucceeds_kafkaNeverCalled() {
    AppProperties properties = propertiesWithTransport(AppProperties.MessageTransport.HTTP);
    FallbackNotificationSender sender =
        new FallbackNotificationSender(httpSender, kafkaSender, properties);

    sender.send(UPDATE);

    verify(httpSender).send(UPDATE);
    verifyNoInteractions(kafkaSender);
  }

  @Test
  void httpTransport_primaryFails_fallsBackToKafka() {
    AppProperties properties = propertiesWithTransport(AppProperties.MessageTransport.HTTP);
    FallbackNotificationSender sender =
        new FallbackNotificationSender(httpSender, kafkaSender, properties);
    doThrow(new RuntimeException("http down")).when(httpSender).send(UPDATE);

    assertThatCode(() -> sender.send(UPDATE)).doesNotThrowAnyException();

    verify(httpSender).send(UPDATE);
    verify(kafkaSender).send(UPDATE);
  }

  @Test
  void kafkaTransport_primarySucceeds_httpNeverCalled() {
    AppProperties properties = propertiesWithTransport(AppProperties.MessageTransport.KAFKA);
    FallbackNotificationSender sender =
        new FallbackNotificationSender(httpSender, kafkaSender, properties);

    sender.send(UPDATE);

    verify(kafkaSender).send(UPDATE);
    verifyNoInteractions(httpSender);
  }

  @Test
  void kafkaTransport_primaryFails_fallsBackToHttp() {
    AppProperties properties = propertiesWithTransport(AppProperties.MessageTransport.KAFKA);
    FallbackNotificationSender sender =
        new FallbackNotificationSender(httpSender, kafkaSender, properties);
    doThrow(new RuntimeException("kafka down")).when(kafkaSender).send(UPDATE);

    assertThatCode(() -> sender.send(UPDATE)).doesNotThrowAnyException();

    verify(kafkaSender).send(UPDATE);
    verify(httpSender).send(UPDATE);
  }

  private static AppProperties propertiesWithTransport(AppProperties.MessageTransport transport) {
    return new AppProperties(
        null, null, null, null, null, transport, null, null, null, null, null, null);
  }
}
