package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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

    @Override
    public void recover(Message message, Throwable cause) {

        Throwable rootCause = getRootCause(cause);
        String queue = message.getMessageProperties().getConsumerQueue();

        Counter.builder("station.rabbit.retries.exhausted")
                .description("Number of Rabbit messages rejected after exhausting retries")
                .tag("queue", queue == null ? "unknown" : queue)
                .register(meterRegistry)
                .increment();

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
