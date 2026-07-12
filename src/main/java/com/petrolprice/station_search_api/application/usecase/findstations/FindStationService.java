package com.petrolprice.station_search_api.application.usecase.findstations;

import com.petrolprice.station_search_api.application.port.out.StationSearchRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindStationService implements FindStationsUseCase{

    private final StationSearchRepositoryPort stationSearchRepositoryPort;

    @Override
    public FindStationsResult find(FindStationsQuery query) {
        return stationSearchRepositoryPort.search(query);
    }
}
