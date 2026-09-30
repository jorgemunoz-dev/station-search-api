package com.petrolprice.station_search_api.station.ingestion.application.command;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CompleteStationPublishingCommand(UUID snapshotId, int publishedStations, Instant completedAt) {}
