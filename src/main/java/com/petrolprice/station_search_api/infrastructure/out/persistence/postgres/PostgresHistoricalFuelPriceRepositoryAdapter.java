package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import com.petrolprice.station_search_api.application.port.out.HistoricalFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.domain.model.FuelPrice;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.HistoricalFuelPriceEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.PostgresHistoricalFuelPriceRepository;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper.HistoricalFuelPriceEntityMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresHistoricalFuelPriceRepositoryAdapter implements HistoricalFuelPriceRepositoryPort {
    private final PostgresHistoricalFuelPriceRepository jpaRepository;
    private final HistoricalFuelPriceEntityMapper mapper;

    @Override
    public void insertSnapshot(UUID stationId, List<FuelPrice> fuelPrices) {
        List<HistoricalFuelPriceEntity> historicalPrices = fuelPrices.stream()
                .map(fuelPrice -> mapper.toEntity(stationId, fuelPrice))
                .toList();

        jpaRepository.saveAll(historicalPrices);
    }
}
