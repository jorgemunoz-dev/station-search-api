package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import com.petrolprice.station_search_api.domain.model.FuelPrice;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.HistoricalFuelPriceEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.PostgresHistoricalFuelPriceRepository;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper.HistoricalFuelPriceEntityMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgresHistoricalFuelPriceRepositoryAdapterTest {
    @Mock
    private PostgresHistoricalFuelPriceRepository jpaRepository;

    @Mock
    private HistoricalFuelPriceEntityMapper mapper;

    @InjectMocks
    private PostgresHistoricalFuelPriceRepositoryAdapter adapter;

    @Test
    void shouldInsertHistoricalSnapshot() {
        // Given
        UUID stationId = UUID.randomUUID();

        FuelPrice diesel = mock(FuelPrice.class);
        FuelPrice gasoline = mock(FuelPrice.class);

        HistoricalFuelPriceEntity dieselEntity = mock(HistoricalFuelPriceEntity.class);
        HistoricalFuelPriceEntity gasolineEntity = mock(HistoricalFuelPriceEntity.class);

        when(mapper.toEntity(stationId, diesel)).thenReturn(dieselEntity);
        when(mapper.toEntity(stationId, gasoline)).thenReturn(gasolineEntity);

        // When
        adapter.insertSnapshot(stationId, List.of(diesel, gasoline));

        // Then
        verify(mapper).toEntity(stationId, diesel);
        verify(mapper).toEntity(stationId, gasoline);
        verify(jpaRepository).saveAll(List.of(dieselEntity, gasolineEntity));
    }

    @Test
    void shouldDoNothingWhenFuelPricesIsEmpty() {
        // When
        adapter.insertSnapshot(UUID.randomUUID(), List.of());

        // Then
        verifyNoInteractions(mapper);
        verifyNoInteractions(jpaRepository);
    }

    @Test
    void shouldDoNothingWhenFuelPricesIsNull() {
        // When
        adapter.insertSnapshot(UUID.randomUUID(), null);

        // Then
        verifyNoInteractions(mapper);
        verifyNoInteractions(jpaRepository);
    }
}