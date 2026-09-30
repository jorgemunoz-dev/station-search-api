package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class RabbitMessageRecovererTest {

    @Test
    void countsExhaustedRetriesByQueueAndRejectsTheMessage() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RabbitMessageRecoverer recoverer = new RabbitMessageRecoverer(meterRegistry);
        MessageProperties properties = new MessageProperties();
        properties.setConsumerQueue("station.snapshot.ingestion.queue");
        properties.setReceivedRoutingKey("energy.snapshot.fuel.es.created");
        Message message = new Message(new byte[0], properties);

        assertThatThrownBy(() -> recoverer.recover(message, new RuntimeException("failed")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessage("Retries exhausted");

        assertThat(meterRegistry.get("station.rabbit.retries.exhausted")
                        .tag("queue", "station.snapshot.ingestion.queue")
                        .counter()
                        .count())
                .isEqualTo(1);
    }
}
