package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import com.petrolprice.station_search_api.application.port.out.HistoricalPriceRepositoryPort;
import com.petrolprice.station_search_api.domain.model.ProductPrice;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.HistoricalFuelPriceEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.JPAHistoricalFuelPriceRepository;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper.HistoricalFuelPriceEntityMapper;
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
    public void insertSnapshot(UUID stationId, List<ProductPrice> productPrices) {
        if (productPrices == null || productPrices.isEmpty()) {
            return;
        }

        List<HistoricalFuelPriceEntity> historicalPrices = productPrices.stream()
                .map(fuelPrice -> mapper.toEntity(stationId, fuelPrice))
                .toList();

        jpaRepository.saveAll(historicalPrices);
    }
}
