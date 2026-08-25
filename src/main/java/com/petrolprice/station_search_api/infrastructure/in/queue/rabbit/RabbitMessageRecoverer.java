package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RabbitMessageRecoverer implements MessageRecoverer {
    @Override
    public void recover(Message message, Throwable cause) {

        Throwable rootCause = getRootCause(cause);

        log.error(
            "Rabbit message processing failed after retries. queue={}, messageId={}, error={} - {}",
            message.getMessageProperties().getConsumerQueue(),
            message.getMessageProperties().getMessageId(),
            rootCause.getClass().getSimpleName(),
            rootCause.getMessage()
        );

        throw new AmqpRejectAndDontRequeueException(
            "Retries exhausted",
            cause
        );
    }



    private Throwable getRootCause(Throwable throwable) {
        Throwable result = throwable;

        while (result.getCause() != null) {
            result = result.getCause();
        }

        return result;
    }
}
