package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa;

import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.HistoricalFuelPriceEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostgresHistoricalFuelPriceRepository extends JpaRepository<HistoricalFuelPriceEntity, UUID> {
    List<HistoricalFuelPriceEntity> findByStationId(UUID stationId);
}
