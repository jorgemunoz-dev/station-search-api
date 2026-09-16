package com.petrolprice.station_search_api.station.ingestion.application;

import com.petrolprice.station_search_api.station.ingestion.application.port.out.CurrentFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.HistoricalPriceRepositoryPort;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationRepositoryPort;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationImportRepositoryPort;
import com.petrolprice.station_search_api.station.ingestion.application.command.ProcessStationSnapshotCommand;
import com.petrolprice.station_search_api.station.domain.model.Station;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessStationSnapshotService {

    private final StationRepositoryPort stationRepositoryPort;
    private final CurrentFuelPriceRepositoryPort currentFuelPriceRepositoryPort;
    private final HistoricalPriceRepositoryPort historicalPriceRepositoryPort;
    private final StationImportRepositoryPort stationImportRepositoryPort;
    private final StationImportFinalizer stationImportFinalizer;

    @Transactional
    public void consume(ProcessStationSnapshotCommand command) {

        stationImportRepositoryPort.ensureExists(
            command.snapshotId()
        );

        boolean claimed = stationImportRepositoryPort.claimEvent(
            command.snapshotId(),
            command.eventId()
        );

        if (!claimed) {
            return;
        }

        Station persitedStation = stationRepositoryPort.upsertFromSnapshot(command.station());
        currentFuelPriceRepositoryPort.replaceCurrentPrices(
                persitedStation.getId(), command.station().getProductPrices());
        historicalPriceRepositoryPort.insertSnapshot(persitedStation.getId(), command.station().getProductPrices());
        stationImportRepositoryPort.incrementProcessedStations(command.snapshotId());
        stationImportFinalizer.tryFinalize(command.snapshotId());
    }
}
