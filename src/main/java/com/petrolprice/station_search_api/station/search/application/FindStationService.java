package com.petrolprice.station_search_api.station.search.application;

import com.petrolprice.station_search_api.station.search.application.port.out.StationSearchRepositoryPort;
import com.petrolprice.station_search_api.station.search.application.query.FindStationsQuery;
import com.petrolprice.station_search_api.station.search.application.result.FindStationsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindStationService implements FindStationsUseCase {

    private final StationSearchRepositoryPort stationSearchRepositoryPort;

    @Override
    public FindStationsResult find(FindStationsQuery query) {
        return stationSearchRepositoryPort.search(query);
    }
}
