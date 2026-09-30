package com.petrolprice.station_search_api.station.infrastructure.postgres.jpa;

import com.petrolprice.station_search_api.station.domain.type.Country;
import com.petrolprice.station_search_api.station.infrastructure.postgres.entity.StationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JPAStationRepository extends JpaRepository<StationEntity, UUID> {
    Optional<StationEntity> findByExternalIdAndCountry(String externalId, Country country);
}
