package com.petrolprice.station_search_api.application.port.out;

import com.petrolprice.station_search_api.domain.model.Station;
import com.petrolprice.station_search_api.domain.type.Country;

public interface StationRepositoryPort {
    void save(Station station);
    boolean existsByExternalIdAndCountry(String externalId, Country country);

}
