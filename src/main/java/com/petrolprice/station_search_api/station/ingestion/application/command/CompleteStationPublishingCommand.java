package com.petrolprice.station_search_api.station.ingestion.application.command;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record CompleteStationPublishingCommand(
    UUID snapshotId,
    int publishedStations,
    Instant completedAt
) {
}
