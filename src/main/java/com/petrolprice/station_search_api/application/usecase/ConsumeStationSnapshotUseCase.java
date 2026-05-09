package com.petrolprice.station_search_api.application.usecase;

import com.petrolprice.station_search_api.application.port.out.StationRepositoryPort;
import com.petrolprice.station_search_api.domain.model.Station;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConsumeStationSnapshotUseCase {

    private final StationRepositoryPort stationRepositoryPort;

    public void consume(Station station) {
        if (!stationRepositoryPort.existsByExternalIdAndCountry(station.getExternalId(), station.getCountry())) {
            stationRepositoryPort.save(station);
        }

    }

}
