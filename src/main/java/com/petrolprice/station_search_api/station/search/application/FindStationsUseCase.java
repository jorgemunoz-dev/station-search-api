package com.petrolprice.station_search_api.station.search.application;

import com.petrolprice.station_search_api.station.search.application.query.FindStationsQuery;
import com.petrolprice.station_search_api.station.search.application.result.FindStationsResult;

public interface FindStationsUseCase {
    FindStationsResult find(FindStationsQuery query);
}
