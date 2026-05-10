package com.petrolprice.station_search_api.application.usecase;

import com.petrolprice.station_search_api.application.port.out.CurrentFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.application.port.out.HistoricalFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.application.port.out.StationRepositoryPort;
import com.petrolprice.station_search_api.domain.model.Station;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConsumeStationSnapshotUseCase {

    private final StationRepositoryPort stationRepositoryPort;
    private final CurrentFuelPriceRepositoryPort currentFuelPriceRepositoryPort;
    private final HistoricalFuelPriceRepositoryPort historicalFuelPriceRepositoryPort;

    @Transactional
    public void consume(Station stationSnapshot) {
        Station persitedStation = stationRepositoryPort.upsertFromSnapshot(stationSnapshot);
        currentFuelPriceRepositoryPort.upsertCurrentPrices(persitedStation.getId(), stationSnapshot.getFuelPrices());
        historicalFuelPriceRepositoryPort.insertSnapshot(persitedStation.getId(), stationSnapshot.getFuelPrices());
    }
}
