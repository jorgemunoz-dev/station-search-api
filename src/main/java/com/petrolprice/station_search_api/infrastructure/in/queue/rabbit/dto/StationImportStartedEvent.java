package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto;

import java.time.Instant;
import java.util.UUID;

public record StationImportStartedEvent(
    UUID snapshotId,
    String countryCode,
    Instant startedAt
) {
}
