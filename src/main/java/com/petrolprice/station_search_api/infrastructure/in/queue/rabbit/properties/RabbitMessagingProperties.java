package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging.rabbit")
public record RabbitMessagingProperties(ExchangeProperties exchange, DlxProperties dlx, List<QueueProperties> queues) {
    public record ExchangeProperties(String name, String type, boolean durable) {}

    public record DlxProperties(String name) {}

    public record QueueProperties(String name, String dlq, List<String> bindings) {}
}
