package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import com.petrolprice.station_search_api.application.port.out.StationRepositoryPort;
import com.petrolprice.station_search_api.domain.model.Station;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.StationEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.JPAStationRepository;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper.StationEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresStationRepository implements StationRepositoryPort {
    private final JPAStationRepository jpaStationRepository;
    private final StationEntityMapper mapper;

    @Override
    public Station save(Station station) {
        StationEntity stationEntity = mapper.toEntity(station);
        StationEntity persistedStation = jpaStationRepository.save(stationEntity);
        return mapper.toDomain(persistedStation);
    }

    @Override
    public Station upsertFromSnapshot(Station stationSnapshot) {
        return jpaStationRepository
                .findByExternalIdAndCountry(stationSnapshot.getExternalId(), stationSnapshot.getCountry())
                .map(existing -> update(existing, stationSnapshot))
                .orElseGet(() -> save(stationSnapshot));
    }

    /**
     * Updates the persisted station using JPA  dirty checking.
     *
     * Opening periods are handled separetely because replacing the whole collection (as do mapstruct)
     * would trigger unnecessary DELETE + INSERT operations.
     */
    private Station update(StationEntity station, Station stationSnapshot) {
        boolean openingPeriodsChanged = !mapper.mapOpeningPeriodsToDomain(station.getOpeningPeriods())
                .equals(stationSnapshot.getOpeningPeriods());

        mapper.updateEntityFromDomain(stationSnapshot, station);

        if (openingPeriodsChanged) {
            station.getOpeningPeriods().clear();
            station.getOpeningPeriods().addAll(mapper.mapOpeningPeriods(stationSnapshot.getOpeningPeriods()));
        }

        return mapper.toDomain(station);
    }
}
