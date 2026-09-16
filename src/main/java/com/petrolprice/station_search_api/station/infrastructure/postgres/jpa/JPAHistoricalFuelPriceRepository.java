package com.petrolprice.station_search_api.station.infrastructure.postgres.jpa;

import com.petrolprice.station_search_api.station.infrastructure.postgres.entity.HistoricalFuelPriceEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JPAHistoricalFuelPriceRepository extends JpaRepository<HistoricalFuelPriceEntity, UUID> {
    List<HistoricalFuelPriceEntity> findByStationId(UUID stationId);
}
