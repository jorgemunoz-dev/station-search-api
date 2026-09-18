package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging.rabbit.retry")
public record RabbitRetryProperties(int maxRetries) {}
