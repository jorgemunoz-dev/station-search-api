package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit;

import java.time.Instant;
import java.util.UUID;

public record StationImportCompletedEvent(
    UUID snapshotId,
    int publishedEvents,
    Instant completedAt
) {}