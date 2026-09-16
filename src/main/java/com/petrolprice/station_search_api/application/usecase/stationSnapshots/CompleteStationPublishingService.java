package com.petrolprice.station_search_api.application.usecase.stationSnapshots;

import com.petrolprice.station_search_api.application.port.out.StationImportRepositoryPort;
import com.petrolprice.station_search_api.application.usecase.stationSnapshots.command.CompleteStationPublishingCommand;
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
        stationImportRepositoryPort.markPublishingCompleted(command.snapshotId(), command.publishedStations(), command.completedAt());
        stationImportFinalizer.tryFinalize(command.snapshotId());
    }
}
