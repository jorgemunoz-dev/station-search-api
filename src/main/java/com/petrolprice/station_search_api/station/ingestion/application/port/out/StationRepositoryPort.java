package com.petrolprice.station_search_api.station.ingestion.application.port.out;

import com.petrolprice.station_search_api.station.domain.model.Station;

public interface StationRepositoryPort {
    Station save(Station station);

    Station upsertFromSnapshot(Station station);

    /** Flushes pending station changes before dependent price rows are inserted. */
    void flush();
}
