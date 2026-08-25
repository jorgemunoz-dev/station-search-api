package com.petrolprice.station_search_api.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.petrolprice.station_search_api.application.port.out.CurrentFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.application.port.out.HistoricalPriceRepositoryPort;
import com.petrolprice.station_search_api.application.port.out.StationRepositoryPort;
import com.petrolprice.station_search_api.domain.model.ProductPrice;
import com.petrolprice.station_search_api.domain.model.Station;
import com.petrolprice.station_search_api.domain.type.Country;
import com.petrolprice.station_search_api.domain.type.ProductType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsumeStationSnapshotUseCaseTest {

    @Mock
    HistoricalPriceRepositoryPort historicalPriceRepositoryPort;

    @Mock
    CurrentFuelPriceRepositoryPort currentFuelPriceRepositoryPort;

    @Mock
    StationRepositoryPort stationRepositoryPort;

    @InjectMocks
    ConsumeStationSnapshotUseCase useCase;

    @Test
    void shouldConsumeStationSnapshot() {
        UUID persistedStationId = UUID.randomUUID();

        List<ProductPrice> productPrices = List.of(ProductPrice.builder()
                .productType(ProductType.DIESEL_A)
                .price(new BigDecimal("1.599"))
                .build());

        Station stationSnapshot = Station.builder()
                .externalId("4375")
                .country(Country.ES)
                .productPrices(productPrices)
                .build();

        Station persistedStation = Station.builder()
                .id(persistedStationId)
                .externalId("4375")
                .country(Country.ES)
                .productPrices(productPrices)
                .build();

        when(stationRepositoryPort.upsertFromSnapshot(stationSnapshot)).thenReturn(persistedStation);

        useCase.consume(stationSnapshot);

        InOrder inOrder = inOrder(stationRepositoryPort, currentFuelPriceRepositoryPort, historicalPriceRepositoryPort);

        inOrder.verify(stationRepositoryPort).upsertFromSnapshot(stationSnapshot);

        inOrder.verify(currentFuelPriceRepositoryPort).replaceCurrentPrices(persistedStationId, productPrices);

        inOrder.verify(historicalPriceRepositoryPort).insertSnapshot(persistedStationId, productPrices);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void shouldNotPersistPricesWhenStationUpsertFails() {
        Station stationSnapshot =
                Station.builder().externalId("4375").country(Country.ES).build();

        when(stationRepositoryPort.upsertFromSnapshot(stationSnapshot))
                .thenThrow(new RuntimeException("Station persistence failed"));

        assertThatThrownBy(() -> useCase.consume(stationSnapshot))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Station persistence failed");

        verifyNoInteractions(currentFuelPriceRepositoryPort);
        verifyNoInteractions(historicalPriceRepositoryPort);
    }
}
