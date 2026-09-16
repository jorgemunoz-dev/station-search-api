package com.petrolprice.station_search_api.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface StationImportRepositoryPort {

    /**
     * Creates the import if it does not already exist.
     */
    void ensureExists(UUID snapshotId);

    /**
     * Atomically registers an event.
     *
     * @return true if this event had not been processed before.
     */
    boolean claimEvent(UUID snapshotId, UUID eventId);

    void incrementProcessedStations(UUID snapshotId);

    void markPublishingCompleted(
        UUID snapshotId,
        int publishedStations,
        Instant completedAt
    );

    /**
     * Atomically changes PROCESSING -> CALCULATING_STATISTICS
     * only if the complete event has arrived and every published
     * station has been processed.
     */
    boolean claimForStatisticsIfReady(UUID snapshotId);

    void markCompleted(UUID snapshotId);

}
