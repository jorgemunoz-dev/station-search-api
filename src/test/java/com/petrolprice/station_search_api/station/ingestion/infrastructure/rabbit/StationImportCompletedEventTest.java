package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StationImportCompletedEventTest {

    @Test
    void shouldExcludeCompletionEventFromPublishedStationCount() {
        var event = new StationImportCompletedEvent(UUID.randomUUID(), 11_491, Instant.now());

        assertThat(event.publishedStations()).isEqualTo(11_490);
    }

    @Test
    void shouldSupportSnapshotsWithoutStations() {
        var event = new StationImportCompletedEvent(UUID.randomUUID(), 1, Instant.now());

        assertThat(event.publishedStations()).isZero();
    }
}
