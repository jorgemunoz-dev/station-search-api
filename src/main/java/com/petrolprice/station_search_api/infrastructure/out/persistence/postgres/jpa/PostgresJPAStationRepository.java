package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa;

import com.petrolprice.station_search_api.domain.type.Country;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.StationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostgresJPAStationRepository extends JpaRepository<StationEntity, UUID> {
    boolean existsByExternalIdAndCountry(String externalId, Country country);
}
