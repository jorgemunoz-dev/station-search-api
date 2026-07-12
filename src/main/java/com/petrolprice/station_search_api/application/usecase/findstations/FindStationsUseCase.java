package com.petrolprice.station_search_api.application.usecase.findstations;

public interface FindStationsUseCase {
    FindStationsResult find(FindStationsQuery query);
}
