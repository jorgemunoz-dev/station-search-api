package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit;

import java.time.Instant;
import java.util.UUID;

public record StationImportCompletedEvent(UUID snapshotId, int publishedEvents, Instant completedAt) {
    public int publishedStations() {
        // The publisher counts the completion event itself in publishedEvents.
        return Math.max(0, publishedEvents - 1);
    }
}
