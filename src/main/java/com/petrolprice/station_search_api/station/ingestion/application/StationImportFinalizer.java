package com.petrolprice.station_search_api.station.ingestion.application;

import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationImportRepositoryPort;
import com.petrolprice.station_search_api.statistics.application.CalculateFuelPriceStatisticsUseCase;
import com.petrolprice.station_search_api.statistics.application.command.CalculateFuelPriceStatisticsCommand;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StationImportFinalizer {

    private final StationImportRepositoryPort stationImportRepositoryPort;
    private final CalculateFuelPriceStatisticsUseCase statisticsCalculator;

    @Transactional
    public void tryFinalize(UUID snapshotId) {
        boolean claimed = stationImportRepositoryPort.claimForStatisticsIfReady(snapshotId);

        if (!claimed) {
            return;
        }

        statisticsCalculator.calculate(new CalculateFuelPriceStatisticsCommand(snapshotId));

        stationImportRepositoryPort.markCompleted(snapshotId);
    }
}
