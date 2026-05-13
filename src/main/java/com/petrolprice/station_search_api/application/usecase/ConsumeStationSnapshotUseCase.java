package com.petrolprice.station_search_api.application.usecase;

import com.petrolprice.station_search_api.application.port.out.CurrentFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.application.port.out.HistoricalFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.application.port.out.StationRepositoryPort;
import com.petrolprice.station_search_api.domain.model.Station;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsumeStationSnapshotUseCase {

    private final StationRepositoryPort stationRepositoryPort;
    private final CurrentFuelPriceRepositoryPort currentFuelPriceRepositoryPort;
    private final HistoricalFuelPriceRepositoryPort historicalFuelPriceRepositoryPort;

    @Transactional
    public void consume(Station stationSnapshot) {
        Station persitedStation = stationRepositoryPort.upsertFromSnapshot(stationSnapshot);
        currentFuelPriceRepositoryPort.replaceCurrentPrices(persitedStation.getId(), stationSnapshot.getFuelPrices());
        historicalFuelPriceRepositoryPort.insertSnapshot(persitedStation.getId(), stationSnapshot.getFuelPrices());
    }
}
