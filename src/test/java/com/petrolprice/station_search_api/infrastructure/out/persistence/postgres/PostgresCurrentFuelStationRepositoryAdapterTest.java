package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import com.petrolprice.station_search_api.domain.model.FuelPrice;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.CurrentFuelPriceEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.PostgresJPACurrentPriceRepository;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper.CurrentPriceEntityMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgresCurrentFuelStationRepositoryAdapterTest {

    @Mock
    private PostgresJPACurrentPriceRepository jpaCurrentPriceRepository;

    @Mock
    private CurrentPriceEntityMapper mapper;

    @InjectMocks
    private PostgresCurrentFuelStationRepositoryAdapter adapter;

    @Test
    void shouldDeleteFlushAndSaveNewCurrentPrices() {
        // Given
        UUID stationId = UUID.randomUUID();

        FuelPrice diesel = mock(FuelPrice.class);
        FuelPrice gasoline = mock(FuelPrice.class);

        CurrentFuelPriceEntity dieselEntity = mock(CurrentFuelPriceEntity.class);
        CurrentFuelPriceEntity gasolineEntity = mock(CurrentFuelPriceEntity.class);

        when(mapper.toEntity(stationId, diesel)).thenReturn(dieselEntity);
        when(mapper.toEntity(stationId, gasoline)).thenReturn(gasolineEntity);

        // When
        adapter.replaceCurrentPrices(stationId, List.of(diesel, gasoline));

        // Then
        InOrder inOrder = inOrder(jpaCurrentPriceRepository);

        inOrder.verify(jpaCurrentPriceRepository).deleteByStationId(stationId);
        inOrder.verify(jpaCurrentPriceRepository).flush();
        inOrder.verify(jpaCurrentPriceRepository).saveAll(List.of(dieselEntity, gasolineEntity));

        verify(mapper).toEntity(stationId, diesel);
        verify(mapper).toEntity(stationId, gasoline);
    }

    @Test
    void shouldOnlyDeleteAndFlushWhenFuelPricesIsNull() {
        // Given
        UUID stationId = UUID.randomUUID();

        // When
        adapter.replaceCurrentPrices(stationId, null);

        // Then
        InOrder inOrder = inOrder(jpaCurrentPriceRepository);

        inOrder.verify(jpaCurrentPriceRepository).deleteByStationId(stationId);
        inOrder.verify(jpaCurrentPriceRepository).flush();

        verifyNoInteractions(mapper);
        verify(jpaCurrentPriceRepository, never()).saveAll(any());
    }

    @Test
    void shouldOnlyDeleteAndFlushWhenFuelPricesIsEmpty() {
        // Given
        UUID stationId = UUID.randomUUID();

        // When
        adapter.replaceCurrentPrices(stationId, List.of());

        // Then
        InOrder inOrder = inOrder(jpaCurrentPriceRepository);

        inOrder.verify(jpaCurrentPriceRepository).deleteByStationId(stationId);
        inOrder.verify(jpaCurrentPriceRepository).flush();

        verifyNoInteractions(mapper);
        verify(jpaCurrentPriceRepository, never()).saveAll(any());
    }

}