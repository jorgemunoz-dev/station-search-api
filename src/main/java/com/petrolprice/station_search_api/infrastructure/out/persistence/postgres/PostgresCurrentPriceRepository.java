package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import com.petrolprice.station_search_api.application.port.out.CurrentFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.domain.model.ProductPrice;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.CurrentFuelPriceEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.JPACurrentPriceRepository;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper.CurrentPriceEntityMapper;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresCurrentPriceRepository implements CurrentFuelPriceRepositoryPort {
    private final JPACurrentPriceRepository currentPriceRepository;
    private final CurrentPriceEntityMapper mapper;

    @Override
    public void replaceCurrentPrices(UUID stationId, List<ProductPrice> productPrices) {
        currentPriceRepository.deleteByStationId(stationId);
        currentPriceRepository.flush();

        if (productPrices == null || productPrices.isEmpty()) {
            return;
        }

        List<CurrentFuelPriceEntity> entities = productPrices.stream()
                .map((price) -> mapper.toEntity(stationId, price))
                .toList();

        currentPriceRepository.saveAll(entities);
    }
}
