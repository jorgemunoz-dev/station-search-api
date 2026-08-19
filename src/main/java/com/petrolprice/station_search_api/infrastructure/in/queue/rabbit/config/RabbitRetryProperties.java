package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.messaging.rabbit.retry")
public record RabbitRetryProperties(
    int maxRetries,
    Duration initialInterval,
    double multiplier,
    Duration maxInterval
) {}