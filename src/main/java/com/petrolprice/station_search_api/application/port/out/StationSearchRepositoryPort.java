package com.petrolprice.station_search_api.application.port.out;

import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsQuery;
import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsResult;

public interface StationSearchRepositoryPort {
    FindStationsResult search(FindStationsQuery query);
}
