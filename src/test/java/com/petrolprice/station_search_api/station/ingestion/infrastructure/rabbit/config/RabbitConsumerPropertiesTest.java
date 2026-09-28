package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.config;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class RabbitConsumerPropertiesTest {

    @Test
    void shouldRejectMaxConcurrencyBelowInitialConcurrency() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RabbitConsumerProperties(8, 4, 100))
                .withMessage("snapshot consumer maxConcurrency must be at least concurrency");
    }

    @Test
    void shouldRejectNonPositivePrefetch() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RabbitConsumerProperties(8, 16, 0))
                .withMessage("snapshot consumer prefetch must be positive");
    }
}
