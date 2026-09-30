package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageBatchRecoverer;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RabbitMessageRecoverer implements MessageRecoverer, MessageBatchRecoverer {
    @Override
    public void recover(Message message, Throwable cause) {

        Throwable rootCause = getRootCause(cause);

        log.error(
                "Rabbit message processing failed after retries. queue={}, messageId={}, error={} - {}",
                message.getMessageProperties().getConsumerQueue(),
                message.getMessageProperties().getMessageId(),
                rootCause.getClass().getSimpleName(),
                rootCause.getMessage());

        throw new AmqpRejectAndDontRequeueException("Retries exhausted", cause);
    }

    @Override
    public void recover(List<Message> messages, Throwable cause) {
        Throwable rootCause = getRootCause(cause);

        log.error(
                "Rabbit batch processing failed after retries. Rejecting all {} messages to their DLQ. error={} - {}",
                messages.size(),
                rootCause.getClass().getSimpleName(),
                rootCause.getMessage());

        throw new AmqpRejectAndDontRequeueException("Batch retries exhausted", cause);
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable result = throwable;

        while (result.getCause() != null) {
            result = result.getCause();
        }

        return result;
    }
}
