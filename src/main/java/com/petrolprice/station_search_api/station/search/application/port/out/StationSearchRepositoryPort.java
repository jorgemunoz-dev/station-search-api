package com.petrolprice.station_search_api.station.search.application.port.out;

import com.petrolprice.station_search_api.station.search.application.query.FindStationsQuery;
import com.petrolprice.station_search_api.station.search.application.result.FindStationsResult;

public interface StationSearchRepositoryPort {
    FindStationsResult search(FindStationsQuery query);
}
