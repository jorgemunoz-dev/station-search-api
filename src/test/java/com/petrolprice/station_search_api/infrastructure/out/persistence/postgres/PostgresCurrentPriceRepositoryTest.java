package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import static org.mockito.Mockito.*;

import com.petrolprice.station_search_api.domain.model.ProductPrice;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.CurrentFuelPriceEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.JPACurrentPriceRepository;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper.CurrentPriceEntityMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresCurrentPriceRepositoryTest {

    @Mock
    private JPACurrentPriceRepository jpaCurrentPriceRepository;

    @Mock
    private CurrentPriceEntityMapper mapper;

    @InjectMocks
    private PostgresCurrentPriceRepository adapter;

    @Test
    void shouldDeleteFlushAndSaveNewCurrentPrices() {
        // Given
        UUID stationId = UUID.randomUUID();

        ProductPrice diesel = mock(ProductPrice.class);
        ProductPrice gasoline = mock(ProductPrice.class);

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
