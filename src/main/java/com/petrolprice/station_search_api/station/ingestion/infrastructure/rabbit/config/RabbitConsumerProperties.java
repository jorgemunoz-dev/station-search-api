package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging.rabbit.consumer.snapshot")
public record RabbitConsumerProperties(int concurrency, int maxConcurrency, int prefetch) {
    public RabbitConsumerProperties {
        if (concurrency < 1) {
            throw new IllegalArgumentException("snapshot consumer concurrency must be positive");
        }
        if (maxConcurrency < concurrency) {
            throw new IllegalArgumentException("snapshot consumer maxConcurrency must be at least concurrency");
        }
        if (prefetch < 1) {
            throw new IllegalArgumentException("snapshot consumer prefetch must be positive");
        }
    }
}
