package com.petrolprice.station_search_api.station.ingestion.application.command;

import com.petrolprice.station_search_api.station.domain.model.Station;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ProcessStationSnapshotCommand(
    UUID eventId,
    UUID snapshotId,
    Station station
) {
}
