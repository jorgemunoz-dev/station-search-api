package com.petrolprice.station_search_api.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.RabbitMessageRecoverer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class RabbitFailureMetricTest {
    @Test
    void firstRejectedMessageExposesTimestampAsWellAsCounter() {
        var registry = new SimpleMeterRegistry();
        var recoverer = new RabbitMessageRecoverer(registry);
        var properties = new MessageProperties();
        properties.setConsumerQueue("station.snapshot.ingestion.queue");
        long before = Instant.now().getEpochSecond();
        try {
            assertThatThrownBy(() -> recoverer.recover(
                            new Message(new byte[0], properties), new IllegalStateException("test failure")))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);
            assertThat(registry.get("station.rabbit.retries.exhausted")
                            .counter()
                            .count())
                    .isEqualTo(1);
            assertThat(registry.get("station.rabbit.last.failure.timestamp")
                            .gauge()
                            .value())
                    .isBetween((double) before, (double) Instant.now().getEpochSecond());
        } finally {
            registry.close();
        }
    }
}
