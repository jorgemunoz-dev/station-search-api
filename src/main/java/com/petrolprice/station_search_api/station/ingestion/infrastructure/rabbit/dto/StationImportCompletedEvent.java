package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto;

import java.time.Instant;
import java.util.UUID;

public record StationImportCompletedEvent(UUID snapshotId, int publishedStations, Instant completedAt) {}
