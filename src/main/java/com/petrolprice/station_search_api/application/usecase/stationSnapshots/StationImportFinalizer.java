package com.petrolprice.station_search_api.application.usecase.stationSnapshots;

import com.petrolprice.station_search_api.application.port.out.StationImportRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StationImportFinalizer {

    private final StationImportRepositoryPort stationImportRepositoryPort;
//    private final DailyFuelPriceStatisticsCalculator statisticsCalculator;

    @Transactional
    public void tryFinalize(UUID snapshotId) {
        boolean claimed =
            stationImportRepositoryPort
                .claimForStatisticsIfReady(snapshotId);

        if (!claimed) {
            return;
        }

//        statisticsCalculator.calculate(snapshotId);

        stationImportRepositoryPort.markCompleted(snapshotId);
    }
}