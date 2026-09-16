package com.petrolprice.station_search_api.station.ingestion.application.port.out;

import com.petrolprice.station_search_api.station.domain.model.OpeningPeriod;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OpeningPeriodsRepositoryPort {
    Map<UUID, List<OpeningPeriod>> getOpeningPeriodsByStationId(UUID stationId);
}
