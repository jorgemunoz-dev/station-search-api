package com.petrolprice.station_search_api.station.infrastructure.postgres;

import static org.mockito.Mockito.*;

import com.petrolprice.station_search_api.station.domain.model.ProductPrice;
import com.petrolprice.station_search_api.station.infrastructure.postgres.entity.HistoricalFuelPriceEntity;
import com.petrolprice.station_search_api.station.infrastructure.postgres.jpa.JPAHistoricalFuelPriceRepository;
import com.petrolprice.station_search_api.station.infrastructure.postgres.mapper.HistoricalFuelPriceEntityMapper;
import java.time.Instant;
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
        UUID snapshotId = UUID.randomUUID();
        Instant observedAt = Instant.parse("2026-09-25T08:00:00Z");

        ProductPrice diesel = mock(ProductPrice.class);
        ProductPrice gasoline = mock(ProductPrice.class);

        HistoricalFuelPriceEntity dieselEntity = mock(HistoricalFuelPriceEntity.class);
        HistoricalFuelPriceEntity gasolineEntity = mock(HistoricalFuelPriceEntity.class);

        when(mapper.toEntity(snapshotId, stationId, observedAt, diesel)).thenReturn(dieselEntity);
        when(mapper.toEntity(snapshotId, stationId, observedAt, gasoline)).thenReturn(gasolineEntity);

        // When
        adapter.insertSnapshot(snapshotId, stationId, observedAt, List.of(diesel, gasoline));

        // Then
        verify(mapper).toEntity(snapshotId, stationId, observedAt, diesel);
        verify(mapper).toEntity(snapshotId, stationId, observedAt, gasoline);
        verify(jpaRepository).saveAllAndFlush(List.of(dieselEntity, gasolineEntity));
    }

    @Test
    void shouldDoNothingWhenFuelPricesIsEmpty() {
        // When
        adapter.insertSnapshot(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), List.of());

        // Then
        verifyNoInteractions(mapper);
        verifyNoInteractions(jpaRepository);
    }

    @Test
    void shouldDoNothingWhenFuelPricesIsNull() {
        // When
        adapter.insertSnapshot(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), null);

        // Then
        verifyNoInteractions(mapper);
        verifyNoInteractions(jpaRepository);
    }
}
