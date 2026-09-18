package com.petrolprice.station_search_api.station.ingestion.application.command;

import com.petrolprice.station_search_api.station.domain.model.Station;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ProcessStationSnapshotCommand(UUID eventId, UUID snapshotId, Station station) {}
