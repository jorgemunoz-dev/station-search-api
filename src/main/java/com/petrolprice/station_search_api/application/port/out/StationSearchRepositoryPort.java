package com.petrolprice.station_search_api.application.port.out;

import com.petrolprice.station_search_api.application.usecase.findstations.query.FindStationsQuery;
import com.petrolprice.station_search_api.application.usecase.findstations.result.FindStationsResult;

public interface StationSearchRepositoryPort {
    FindStationsResult search(FindStationsQuery query);
}
