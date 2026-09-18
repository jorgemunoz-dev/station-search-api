package com.petrolprice.station_search_api.station.infrastructure.postgres;

import com.petrolprice.station_search_api.station.domain.model.ProductPrice;
import com.petrolprice.station_search_api.station.infrastructure.postgres.entity.HistoricalFuelPriceEntity;
import com.petrolprice.station_search_api.station.infrastructure.postgres.jpa.JPAHistoricalFuelPriceRepository;
import com.petrolprice.station_search_api.station.infrastructure.postgres.mapper.HistoricalFuelPriceEntityMapper;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.HistoricalPriceRepositoryPort;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresHistoricalPriceRepository implements HistoricalPriceRepositoryPort {
    private final JPAHistoricalFuelPriceRepository jpaRepository;
    private final HistoricalFuelPriceEntityMapper mapper;

    @Override
    public void insertSnapshot(UUID snapshotId, UUID stationId, List<ProductPrice> productPrices) {
        if (productPrices == null || productPrices.isEmpty()) {
            return;
        }

        List<HistoricalFuelPriceEntity> historicalPrices = productPrices.stream()
                .map(fuelPrice -> mapper.toEntity(snapshotId, stationId, fuelPrice))
                .toList();

        // The final station event can trigger JDBC aggregation in the same transaction.
        // Flush here so the snapshot rows are visible to that calculation.
        jpaRepository.saveAllAndFlush(historicalPrices);
    }
}
