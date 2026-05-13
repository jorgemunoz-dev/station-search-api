package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import com.petrolprice.station_search_api.application.port.out.CurrentFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.domain.model.FuelPrice;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.CurrentFuelPriceEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.PostgresJPACurrentPriceRepository;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper.CurrentPriceEntityMapper;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresCurrentFuelStationRepositoryAdapter implements CurrentFuelPriceRepositoryPort {
    private final PostgresJPACurrentPriceRepository currentPriceRepository;
    private final CurrentPriceEntityMapper mapper;

    @Override
    @Transactional
    public void replaceCurrentPrices(UUID stationId, List<FuelPrice> fuelPrices) {
        currentPriceRepository.deleteByStationId(stationId);
        currentPriceRepository.flush();

        if (fuelPrices == null || fuelPrices.isEmpty()) {
            return;
        }

        List<CurrentFuelPriceEntity> entities = fuelPrices.stream()
                .map((price) -> mapper.toEntity(stationId, price))
                .toList();

        currentPriceRepository.saveAll(entities);
    }
}
