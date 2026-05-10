package com.petrolprice.station_search_api.application.port.out;

import com.petrolprice.station_search_api.domain.model.Station;
import com.petrolprice.station_search_api.domain.type.Country;

import java.util.Optional;

public interface StationRepositoryPort {
    Station save(Station station);

    Optional<Station> findByExternalIdAndCountry(String externalId, Country country);

    Station upsertFromSnapshot(Station station);

}
