package com.petrolprice.station_search_api.application.usecase;

import com.petrolprice.station_search_api.application.port.out.CurrentFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.application.port.out.HistoricalFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.application.port.out.StationRepositoryPort;
import com.petrolprice.station_search_api.domain.model.Station;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConsumeStationSnapshotUseCase {

    private final StationRepositoryPort stationRepositoryPort;
    private final CurrentFuelPriceRepositoryPort currentFuelPriceRepositoryPort;
    private final HistoricalFuelPriceRepositoryPort historicalFuelPriceRepositoryPort;

    public void consume(Station stationSnapshot) {
        Station persitedStation = stationRepositoryPort
                .findByExternalIdAndCountry(stationSnapshot.getExternalId(), stationSnapshot.getCountry())
                .orElseGet(() -> stationRepositoryPort.save(stationSnapshot));

        currentFuelPriceRepositoryPort.upsertCurrentPrice(persitedStation.getId(), stationSnapshot.getFuelPrices());
        historicalFuelPriceRepositoryPort.insertSnapshot(persitedStation.getId(), stationSnapshot.getFuelPrices());
    }
}
