package com.petrolprice.station_search_api.station.ingestion.application.port.out;

import com.petrolprice.station_search_api.station.domain.model.Address;
import com.petrolprice.station_search_api.station.domain.type.Country;

public interface GeographicAreaResolverPort {
    Resolution resolve(Country country, Address address);

    record Resolution(Long adminArea1Id, Long adminArea2Id, Long adminArea3Id, Long localityId) {}
}
