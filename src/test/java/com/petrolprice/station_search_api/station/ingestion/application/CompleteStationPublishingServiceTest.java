package com.petrolprice.station_search_api.station.ingestion.application;

import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationImportRepositoryPort;
import com.petrolprice.station_search_api.station.ingestion.application.CompleteStationPublishingService;
import com.petrolprice.station_search_api.station.ingestion.application.StationImportFinalizer;
import com.petrolprice.station_search_api.station.ingestion.application.command.CompleteStationPublishingCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompleteStationPublishingServiceTest {

    @Mock
    StationImportRepositoryPort stationImportRepositoryPort;

    @Mock
    StationImportFinalizer stationImportFinalizer;

    @InjectMocks
    CompleteStationPublishingService service;

    @Test
    void shouldCompleteStationPublishingSuccessfully() {
        CompleteStationPublishingCommand command = createCommand();

        service.complete(command);

        InOrder inOrder = inOrder(
            stationImportRepositoryPort,
            stationImportFinalizer
        );

        inOrder.verify(stationImportRepositoryPort)
            .markPublishingCompleted(
                command.snapshotId(),
                command.publishedStations(),
                command.completedAt()
            );

        inOrder.verify(stationImportFinalizer)
            .tryFinalize(command.snapshotId());

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void shouldNotFinalizeSnapshotWhenMarkPublishingCompletedFails() {
        CompleteStationPublishingCommand command = createCommand();

        doThrow(new RuntimeException("Publishing completion failed"))
            .when(stationImportRepositoryPort)
            .markPublishingCompleted(
                command.snapshotId(),
                command.publishedStations(),
                command.completedAt()
            );

        assertThatThrownBy(() -> service.complete(command))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Publishing completion failed");

        verifyNoInteractions(stationImportFinalizer);
    }

    @Test
    void shouldPropagateExceptionWhenFinalizationFails() {
        CompleteStationPublishingCommand command = createCommand();

        doThrow(new RuntimeException("Snapshot finalization failed"))
            .when(stationImportFinalizer)
            .tryFinalize(command.snapshotId());

        assertThatThrownBy(() -> service.complete(command))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Snapshot finalization failed");

        verify(stationImportRepositoryPort)
            .markPublishingCompleted(
                command.snapshotId(),
                command.publishedStations(),
                command.completedAt()
            );
    }

    private CompleteStationPublishingCommand createCommand() {
        return CompleteStationPublishingCommand.builder()
            .snapshotId(UUID.randomUUID())
            .publishedStations(150)
            .completedAt(Instant.now())
            .build();
    }
}
