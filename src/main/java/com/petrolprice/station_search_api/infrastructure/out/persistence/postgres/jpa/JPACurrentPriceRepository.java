package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa;

import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.CurrentFuelPriceEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JPACurrentPriceRepository extends JpaRepository<CurrentFuelPriceEntity, UUID> {

    void deleteByStationId(UUID stationId);

    List<CurrentFuelPriceEntity> findByStationId(UUID stationId);
}
