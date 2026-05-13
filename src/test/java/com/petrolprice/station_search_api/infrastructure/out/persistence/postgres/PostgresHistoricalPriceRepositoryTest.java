package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import static org.mockito.Mockito.*;

import com.petrolprice.station_search_api.domain.model.ProductPrice;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.HistoricalFuelPriceEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.JPAHistoricalFuelPriceRepository;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper.HistoricalFuelPriceEntityMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresHistoricalPriceRepositoryTest {
    @Mock
    private JPAHistoricalFuelPriceRepository jpaRepository;

    @Mock
    private HistoricalFuelPriceEntityMapper mapper;

    @InjectMocks
    private PostgresHistoricalPriceRepository adapter;

    @Test
    void shouldInsertHistoricalSnapshot() {
        // Given
        UUID stationId = UUID.randomUUID();

        ProductPrice diesel = mock(ProductPrice.class);
        ProductPrice gasoline = mock(ProductPrice.class);

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
