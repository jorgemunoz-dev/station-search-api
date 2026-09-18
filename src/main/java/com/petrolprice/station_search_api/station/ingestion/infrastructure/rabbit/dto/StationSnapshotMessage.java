package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto;

import java.time.Instant;
import java.util.UUID;

public record StationSnapshotMessage(
        UUID batchId,
        UUID eventId,
        String eventType,
        String messageVersion,
        Instant occurredAt,
        StationSnapshotPayload payload,
        String source) {}
