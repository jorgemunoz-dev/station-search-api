package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit;

import java.time.Instant;
import java.util.UUID;

public record StationImportCompletedEvent(UUID snapshotId, int publishedEvents, Instant completedAt) {}
