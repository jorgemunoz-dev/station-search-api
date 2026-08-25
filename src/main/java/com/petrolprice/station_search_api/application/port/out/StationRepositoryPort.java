package com.petrolprice.station_search_api.application.port.out;

import com.petrolprice.station_search_api.domain.model.Station;

public interface StationRepositoryPort {
    Station save(Station station);

    Station upsertFromSnapshot(Station station);
}
