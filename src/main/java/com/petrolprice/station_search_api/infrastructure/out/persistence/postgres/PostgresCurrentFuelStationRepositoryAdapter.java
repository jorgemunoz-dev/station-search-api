package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import com.petrolprice.station_search_api.application.port.out.CurrentFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.domain.model.FuelPrice;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.PostgresJPACurrentPriceRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresCurrentFuelStationRepositoryAdapter implements CurrentFuelPriceRepositoryPort {
    private final PostgresJPACurrentPriceRepository currentPriceRepository;

    @Override
    @Transactional
    public void upsertCurrentPrices(UUID stationId, List<FuelPrice> fuelPrices) {
        if (fuelPrices == null || fuelPrices.isEmpty()) {
            return;
        }

        fuelPrices.forEach(fuelPrice ->
            currentPriceRepository.upsertCurrentPrice(
                stationId,
                fuelPrice.getStationProductType().name(),
                fuelPrice.getPrice()
            )
        );
    }
}
