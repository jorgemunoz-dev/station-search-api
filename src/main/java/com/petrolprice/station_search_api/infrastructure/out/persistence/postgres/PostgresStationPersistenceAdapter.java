package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import com.petrolprice.station_search_api.application.port.out.StationRepositoryPort;
import com.petrolprice.station_search_api.domain.model.Station;
import com.petrolprice.station_search_api.domain.type.Country;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.StationEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.PostgresJPAStationRepository;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper.StationEntityMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresStationPersistenceAdapter implements StationRepositoryPort {
    private final PostgresJPAStationRepository jpaStationRepository;
    private final StationEntityMapper mapper;

    @Override
    public Station save(Station station) {
        StationEntity stationEntity = mapper.toEntity(station);
        StationEntity persistedStation = jpaStationRepository.save(stationEntity);
        return mapper.toDomain(persistedStation);
    }

    @Override
    public Optional<Station> findByExternalIdAndCountry(String externalId, Country country) {
        return jpaStationRepository
                .findByExternalIdAndCountry(externalId, country)
                .map(mapper::toDomain);
    }
}
