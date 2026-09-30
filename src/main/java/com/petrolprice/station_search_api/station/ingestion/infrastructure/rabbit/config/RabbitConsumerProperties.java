package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging.rabbit.consumer")
public record RabbitConsumerProperties(Snapshot snapshot, Completion completion) {

    public RabbitConsumerProperties {
        if (snapshot == null || completion == null) {
            throw new IllegalArgumentException("consumer settings are required");
        }
    }

    public record Snapshot(int concurrency, int maxConcurrency, int prefetch, int batchSize) {

        public Snapshot {
            valid(concurrency, prefetch);
            if (maxConcurrency < concurrency) {
                throw new IllegalArgumentException("snapshot maxConcurrency must be >= concurrency");
            }
            if (batchSize <= 0 || batchSize > prefetch) {
                throw new IllegalArgumentException("snapshot batchSize must be positive and <= prefetch");
            }
        }
    }

    public record Completion(int concurrency, int prefetch) {

        public Completion {
            valid(concurrency, prefetch);
        }
    }

    private static void valid(int concurrency, int prefetch) {
        if (concurrency <= 0 || prefetch <= 0) {
            throw new IllegalArgumentException("consumer concurrency and prefetch must be positive");
        }
    }
}
