package com.petrolprice.station_search_api.station.ingestion.application;

import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationImportRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StationImportFinalizationScheduler {

    private static final int PAGE_SIZE = 100;

    private final StationImportRepositoryPort stationImportRepositoryPort;
    private final StationImportFinalizer stationImportFinalizer;

    @Scheduled(fixedDelayString = "${app.messaging.rabbit.import-finalization-delay-ms:1000}")
    public void finalizeReadyImports() {
        stationImportRepositoryPort.findReadyForStatistics(PAGE_SIZE).forEach(stationImportFinalizer::tryFinalize);
    }
}
