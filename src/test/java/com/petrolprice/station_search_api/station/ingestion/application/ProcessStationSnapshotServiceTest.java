package com.petrolprice.station_search_api.station.ingestion.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.petrolprice.station_search_api.station.domain.model.ProductPrice;
import com.petrolprice.station_search_api.station.domain.model.Station;
import com.petrolprice.station_search_api.station.domain.type.Country;
import com.petrolprice.station_search_api.station.domain.type.ProductType;
import com.petrolprice.station_search_api.station.ingestion.application.command.ProcessStationSnapshotCommand;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.CurrentFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.HistoricalPriceRepositoryPort;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationImportRepositoryPort;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationRepositoryPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessStationSnapshotServiceTest {

    @Mock
    HistoricalPriceRepositoryPort historicalPriceRepositoryPort;

    @Mock
    CurrentFuelPriceRepositoryPort currentFuelPriceRepositoryPort;

    @Mock
    StationRepositoryPort stationRepositoryPort;

    @Mock
    StationImportRepositoryPort stationImportRepositoryPort;

    @Mock
    StationImportFinalizer stationImportFinalizer;

    @InjectMocks
    ProcessStationSnapshotService service;

    @Nested
    class WhenEventCanBeProcessed {

        @Test
        void shouldProcessStationSnapshotSuccessfully() {
            Station stationSnapshot = createStationSnapshot();
            Station persistedStation = createPersistedStation(stationSnapshot);
            ProcessStationSnapshotCommand command = createCommand(stationSnapshot);

            when(stationImportRepositoryPort.claimEvent(command.snapshotId(), command.eventId()))
                    .thenReturn(true);

            when(stationRepositoryPort.upsertFromSnapshot(stationSnapshot)).thenReturn(persistedStation);

            service.consume(command);

            InOrder inOrder = inOrder(
                    stationImportRepositoryPort,
                    stationRepositoryPort,
                    currentFuelPriceRepositoryPort,
                    historicalPriceRepositoryPort,
                    stationImportFinalizer);

            inOrder.verify(stationImportRepositoryPort)
                    .ensureExists(
                            command.snapshotId(), command.station().getCountry().name());

            inOrder.verify(stationImportRepositoryPort).claimEvent(command.snapshotId(), command.eventId());

            inOrder.verify(stationRepositoryPort).upsertFromSnapshot(stationSnapshot);

            inOrder.verify(currentFuelPriceRepositoryPort)
                    .replaceCurrentPrices(persistedStation.getId(), stationSnapshot.getProductPrices());

            inOrder.verify(historicalPriceRepositoryPort)
                    .insertSnapshot(command.snapshotId(), persistedStation.getId(), stationSnapshot.getProductPrices());

            inOrder.verify(stationImportRepositoryPort).incrementProcessedStations(command.snapshotId());

            inOrder.verify(stationImportFinalizer).tryFinalize(command.snapshotId());

            inOrder.verifyNoMoreInteractions();
        }

        @Test
        void shouldNotPersistPricesWhenStationUpsertFails() {
            Station stationSnapshot = createStationSnapshot();
            ProcessStationSnapshotCommand command = createCommand(stationSnapshot);

            when(stationImportRepositoryPort.claimEvent(command.snapshotId(), command.eventId()))
                    .thenReturn(true);

            when(stationRepositoryPort.upsertFromSnapshot(stationSnapshot))
                    .thenThrow(new RuntimeException("Station persistence failed"));

            assertThatThrownBy(() -> service.consume(command))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Station persistence failed");

            verifyNoInteractions(currentFuelPriceRepositoryPort);
            verifyNoInteractions(historicalPriceRepositoryPort);
            verifyNoInteractions(stationImportFinalizer);

            verify(stationImportRepositoryPort, never()).incrementProcessedStations(any());
        }

        @Test
        void shouldNotPersistHistoricalPricesWhenCurrentPricePersistenceFails() {
            Station stationSnapshot = createStationSnapshot();
            Station persistedStation = createPersistedStation(stationSnapshot);
            ProcessStationSnapshotCommand command = createCommand(stationSnapshot);

            when(stationImportRepositoryPort.claimEvent(command.snapshotId(), command.eventId()))
                    .thenReturn(true);

            when(stationRepositoryPort.upsertFromSnapshot(stationSnapshot)).thenReturn(persistedStation);

            doThrow(new RuntimeException("Current price persistence failed"))
                    .when(currentFuelPriceRepositoryPort)
                    .replaceCurrentPrices(persistedStation.getId(), stationSnapshot.getProductPrices());

            assertThatThrownBy(() -> service.consume(command))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Current price persistence failed");

            verifyNoInteractions(historicalPriceRepositoryPort);
            verifyNoInteractions(stationImportFinalizer);

            verify(stationImportRepositoryPort, never()).incrementProcessedStations(any());
        }

        @Test
        void shouldNotIncrementProcessedStationsWhenHistoricalPricePersistenceFails() {
            Station stationSnapshot = createStationSnapshot();
            Station persistedStation = createPersistedStation(stationSnapshot);
            ProcessStationSnapshotCommand command = createCommand(stationSnapshot);

            when(stationImportRepositoryPort.claimEvent(command.snapshotId(), command.eventId()))
                    .thenReturn(true);

            when(stationRepositoryPort.upsertFromSnapshot(stationSnapshot)).thenReturn(persistedStation);

            doThrow(new RuntimeException("Historical price persistence failed"))
                    .when(historicalPriceRepositoryPort)
                    .insertSnapshot(command.snapshotId(), persistedStation.getId(), stationSnapshot.getProductPrices());

            assertThatThrownBy(() -> service.consume(command))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Historical price persistence failed");

            verify(stationImportRepositoryPort, never()).incrementProcessedStations(any());

            verifyNoInteractions(stationImportFinalizer);
        }

        @Test
        void shouldNotFinalizeSnapshotWhenIncrementProcessedStationsFails() {
            Station stationSnapshot = createStationSnapshot();
            Station persistedStation = createPersistedStation(stationSnapshot);
            ProcessStationSnapshotCommand command = createCommand(stationSnapshot);

            when(stationImportRepositoryPort.claimEvent(command.snapshotId(), command.eventId()))
                    .thenReturn(true);

            when(stationRepositoryPort.upsertFromSnapshot(stationSnapshot)).thenReturn(persistedStation);

            doThrow(new RuntimeException("Processed stations increment failed"))
                    .when(stationImportRepositoryPort)
                    .incrementProcessedStations(command.snapshotId());

            assertThatThrownBy(() -> service.consume(command))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Processed stations increment failed");

            verifyNoInteractions(stationImportFinalizer);
        }
    }

    @Nested
    class WhenEventWasAlreadyProcessed {

        @Test
        void shouldNotProcessStationSnapshot() {
            Station stationSnapshot = createStationSnapshot();
            ProcessStationSnapshotCommand command = createCommand(stationSnapshot);

            when(stationImportRepositoryPort.claimEvent(command.snapshotId(), command.eventId()))
                    .thenReturn(false);

            service.consume(command);

            verify(stationImportRepositoryPort)
                    .ensureExists(
                            command.snapshotId(), command.station().getCountry().name());

            verify(stationImportRepositoryPort).claimEvent(command.snapshotId(), command.eventId());

            verifyNoInteractions(stationRepositoryPort);
            verifyNoInteractions(currentFuelPriceRepositoryPort);
            verifyNoInteractions(historicalPriceRepositoryPort);
            verifyNoInteractions(stationImportFinalizer);

            verify(stationImportRepositoryPort, never()).incrementProcessedStations(any());
        }
    }

    private Station createStationSnapshot() {
        return Station.builder()
                .externalId("4375")
                .country(Country.ES)
                .productPrices(List.of(ProductPrice.builder()
                        .productType(ProductType.DIESEL_A)
                        .price(new BigDecimal("1.599"))
                        .build()))
                .build();
    }

    private Station createPersistedStation(Station snapshot) {
        return Station.builder()
                .id(UUID.randomUUID())
                .externalId(snapshot.getExternalId())
                .country(snapshot.getCountry())
                .productPrices(snapshot.getProductPrices())
                .build();
    }

    private ProcessStationSnapshotCommand createCommand(Station station) {
        return ProcessStationSnapshotCommand.builder()
                .snapshotId(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .station(station)
                .build();
    }
}
