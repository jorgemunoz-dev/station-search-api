package com.petrolprice.station_search_api.station.ingestion.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationImportRepositoryPort;
import com.petrolprice.station_search_api.statistics.application.CalculateFuelPriceStatisticsUseCase;
import com.petrolprice.station_search_api.statistics.application.command.CalculateFuelPriceStatisticsCommand;
import java.util.UUID;

import com.petrolprice.station_search_api.station.ingestion.application.StationImportFinalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StationImportFinalizerTest {

    @Mock
    StationImportRepositoryPort stationImportRepositoryPort;

    @Mock
    CalculateFuelPriceStatisticsUseCase statisticsCalculator;

    @InjectMocks
    StationImportFinalizer finalizer;

    @Test
    void shouldMarkSnapshotAsCompletedWhenClaimSucceeds() {
        UUID snapshotId = UUID.randomUUID();

        when(stationImportRepositoryPort.claimForStatisticsIfReady(snapshotId))
            .thenReturn(true);

        finalizer.tryFinalize(snapshotId);

        InOrder inOrder = inOrder(stationImportRepositoryPort, statisticsCalculator);

        inOrder.verify(stationImportRepositoryPort)
            .claimForStatisticsIfReady(snapshotId);

        inOrder.verify(statisticsCalculator)
            .calculate(new CalculateFuelPriceStatisticsCommand(snapshotId));

        inOrder.verify(stationImportRepositoryPort)
            .markCompleted(snapshotId);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void shouldNotMarkSnapshotAsCompletedWhenClaimFails() {
        UUID snapshotId = UUID.randomUUID();

        when(stationImportRepositoryPort.claimForStatisticsIfReady(snapshotId))
            .thenReturn(false);

        finalizer.tryFinalize(snapshotId);

        verify(stationImportRepositoryPort)
            .claimForStatisticsIfReady(snapshotId);

        verify(stationImportRepositoryPort, never())
            .markCompleted(any());

        verifyNoInteractions(statisticsCalculator);

        verifyNoMoreInteractions(stationImportRepositoryPort);
    }

    @Test
    void shouldPropagateExceptionWhenMarkCompletedFails() {
        UUID snapshotId = UUID.randomUUID();

        when(stationImportRepositoryPort.claimForStatisticsIfReady(snapshotId))
            .thenReturn(true);

        doThrow(new RuntimeException("Snapshot completion failed"))
            .when(stationImportRepositoryPort)
            .markCompleted(snapshotId);

        assertThatThrownBy(() -> finalizer.tryFinalize(snapshotId))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Snapshot completion failed");

        verify(stationImportRepositoryPort)
            .claimForStatisticsIfReady(snapshotId);

        verify(statisticsCalculator)
            .calculate(new CalculateFuelPriceStatisticsCommand(snapshotId));

        verify(stationImportRepositoryPort)
            .markCompleted(snapshotId);
    }

    @Test
    void shouldNotCompleteWhenStatisticsCalculationFails() {
        UUID snapshotId = UUID.randomUUID();
        CalculateFuelPriceStatisticsCommand command = new CalculateFuelPriceStatisticsCommand(snapshotId);
        when(stationImportRepositoryPort.claimForStatisticsIfReady(snapshotId)).thenReturn(true);
        doThrow(new RuntimeException("Statistics failed")).when(statisticsCalculator).calculate(command);

        assertThatThrownBy(() -> finalizer.tryFinalize(snapshotId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Statistics failed");

        verify(stationImportRepositoryPort, never()).markCompleted(snapshotId);
    }
}
