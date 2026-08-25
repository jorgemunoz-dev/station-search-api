package com.petrolprice.station_search_api.application.usecase.findstations;

import com.petrolprice.station_search_api.application.usecase.findstations.query.FindStationsQuery;
import com.petrolprice.station_search_api.application.usecase.findstations.result.FindStationsResult;

public interface FindStationsUseCase {
    FindStationsResult find(FindStationsQuery query);
}
