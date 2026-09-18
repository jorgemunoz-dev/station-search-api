package com.petrolprice.station_search_api.station.ingestion.application;

import com.petrolprice.station_search_api.station.ingestion.application.command.CompleteStationPublishingCommand;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationImportRepositoryPort;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompleteStationPublishingService implements CompleteStationPublishingUseCase {

    private final StationImportRepositoryPort stationImportRepositoryPort;
    private final StationImportFinalizer stationImportFinalizer;

    @Override
    @Transactional
    public void complete(CompleteStationPublishingCommand command) {
        stationImportRepositoryPort.markPublishingCompleted(
                command.snapshotId(), command.publishedStations(), command.completedAt());
        stationImportFinalizer.tryFinalize(command.snapshotId());
    }
}
