package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RabbitConsumerPropertiesTest {

    @Test
    void rejectsNonPositiveValues() {
        assertThatThrownBy(() -> new RabbitConsumerProperties.Snapshot(0, 1, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMaxBelowConcurrency() {
        assertThatThrownBy(() -> new RabbitConsumerProperties.Snapshot(8, 4, 100, 100))
                .hasMessageContaining("maxConcurrency");
    }

    @Test
    void rejectsBatchAbovePrefetch() {
        assertThatThrownBy(() -> new RabbitConsumerProperties.Snapshot(8, 16, 100, 101))
                .hasMessageContaining("batchSize");
    }

    @Test
    void acceptsRecommendedValues() {
        RabbitConsumerProperties properties = new RabbitConsumerProperties(
                new RabbitConsumerProperties.Snapshot(8, 16, 500, 100),
                new RabbitConsumerProperties.Completion(1, 1));

        assertThat(properties.snapshot().prefetch()).isEqualTo(500);
        assertThat(properties.snapshot().batchSize()).isEqualTo(100);
    }
}
