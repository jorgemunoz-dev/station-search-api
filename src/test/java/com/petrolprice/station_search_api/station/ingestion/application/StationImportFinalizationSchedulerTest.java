package com.petrolprice.station_search_api.station.ingestion.application;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationImportRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StationImportFinalizationSchedulerTest {

    @Mock
    StationImportRepositoryPort stationImportRepositoryPort;

    @Mock
    StationImportFinalizer stationImportFinalizer;

    @InjectMocks
    StationImportFinalizationScheduler scheduler;

    @Test
    void shouldFinalizeEveryReadyImport() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(stationImportRepositoryPort.findReadyForStatistics(100)).thenReturn(List.of(first, second));

        scheduler.finalizeReadyImports();

        InOrder inOrder = inOrder(stationImportRepositoryPort, stationImportFinalizer);
        inOrder.verify(stationImportRepositoryPort).findReadyForStatistics(100);
        inOrder.verify(stationImportFinalizer).tryFinalize(first);
        inOrder.verify(stationImportFinalizer).tryFinalize(second);
        inOrder.verifyNoMoreInteractions();
    }
}
