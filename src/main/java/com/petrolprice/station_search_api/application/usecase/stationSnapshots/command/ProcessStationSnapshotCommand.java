package com.petrolprice.station_search_api.application.usecase.stationSnapshots.command;

import com.petrolprice.station_search_api.domain.model.Station;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ProcessStationSnapshotCommand(
    UUID eventId,
    UUID snapshotId,
    Station station
) {
}
