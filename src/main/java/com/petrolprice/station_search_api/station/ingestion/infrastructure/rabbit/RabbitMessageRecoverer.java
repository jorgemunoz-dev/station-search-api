package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitMessageRecoverer implements MessageRecoverer {
    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, AtomicLong> lastFailures = new ConcurrentHashMap<>();

    @Override
    public void recover(Message message, Throwable cause) {

        Throwable rootCause = getRootCause(cause);
        String queue = message.getMessageProperties().getConsumerQueue();

        Counter.builder("station.rabbit.retries.exhausted")
                .description("Number of Rabbit messages rejected after exhausting retries")
                .tag("queue", queue == null ? "unknown" : queue)
                .register(meterRegistry)
                .increment();

        // A lazy counter starts at one on its first scrape, so increase() can
        // miss the first rejected message. A timestamp also catches that event.
        lastFailures
                .computeIfAbsent(queue == null ? "unknown" : queue, name -> {
                    AtomicLong value = new AtomicLong();
                    Gauge.builder("station.rabbit.last.failure.timestamp", value, AtomicLong::doubleValue)
                            .description("Epoch seconds of the last message rejected after retries")
                            .baseUnit("seconds")
                            .tag("queue", name)
                            .register(meterRegistry);
                    return value;
                })
                .set(Instant.now().getEpochSecond());

        log.atError()
                .setCause(rootCause)
                .addKeyValue("event", "rabbit_message_retries_exhausted")
                .addKeyValue("queue", queue)
                .addKeyValue("messageId", message.getMessageProperties().getMessageId())
                .addKeyValue("correlationId", message.getMessageProperties().getCorrelationId())
                .addKeyValue("routingKey", message.getMessageProperties().getReceivedRoutingKey())
                .addKeyValue("exceptionType", rootCause.getClass().getName())
                .log("Rabbit message processing failed after retries");

        throw new AmqpRejectAndDontRequeueException("Retries exhausted", cause);
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable result = throwable;

        while (result.getCause() != null) {
            result = result.getCause();
        }

        return result;
    }
}
