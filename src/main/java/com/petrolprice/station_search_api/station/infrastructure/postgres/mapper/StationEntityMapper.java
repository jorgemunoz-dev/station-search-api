package com.petrolprice.station_search_api.station.infrastructure.postgres.mapper;

import com.petrolprice.station_search_api.station.domain.model.OpeningPeriod;
import com.petrolprice.station_search_api.station.domain.model.Station;
import com.petrolprice.station_search_api.station.infrastructure.postgres.entity.StationEntity;
import com.petrolprice.station_search_api.station.infrastructure.postgres.entity.StationOpeningPeriodEntity;
import java.util.List;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        uses = {GeoLocationEntityMapper.class})
public interface StationEntityMapper {

    /**
     * ===========================
     * DOMAIN TO ENTITY
     * ===========================
     */
    @Mapping(target = "street", source = "address.street")
    @Mapping(target = "postalCode", source = "address.postalCode")
    @Mapping(target = "localityId", source = "address.localityId")
    @Mapping(target = "adminArea1Id", source = "address.adminArea1Id")
    @Mapping(target = "adminArea2Id", source = "address.adminArea2Id")
    @Mapping(target = "adminArea3Id", source = "address.adminArea3Id")
    @Mapping(target = "openingPeriods", source = "station.openingPeriods", qualifiedByName = "mapOpeningPeriods")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StationEntity toEntity(Station station);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "street", source = "address.street")
    @Mapping(target = "postalCode", source = "address.postalCode")
    @Mapping(target = "localityId", source = "address.localityId")
    @Mapping(target = "adminArea1Id", source = "address.adminArea1Id")
    @Mapping(target = "adminArea2Id", source = "address.adminArea2Id")
    @Mapping(target = "adminArea3Id", source = "address.adminArea3Id")
    @Mapping(target = "openingPeriods", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDomain(Station station, @MappingTarget StationEntity entity);

    @Named("mapOpeningPeriods")
    default List<StationOpeningPeriodEntity> mapOpeningPeriods(List<OpeningPeriod> openingPeriods) {
        if (openingPeriods == null) {
            return List.of();
        }

        return openingPeriods.stream()
                .filter(openingPeriod -> openingPeriod.getDays() != null)
                .flatMap(openingPeriod -> openingPeriod.getDays().stream()
                        .map(day -> StationOpeningPeriodEntity.builder()
                                .day(day)
                                .open(openingPeriod.getOpen())
                                .close(openingPeriod.getClose())
                                .build()))
                .toList();
    }

    /**
     * ===========================
     * ENTITY TO DOMAIN
     * ===========================
     */
    @Mapping(target = "address.street", source = "street")
    @Mapping(target = "address.postalCode", source = "postalCode")
    @Mapping(target = "address.localityId", source = "localityId")
    @Mapping(target = "address.adminArea1Id", source = "adminArea1Id")
    @Mapping(target = "address.adminArea2Id", source = "adminArea2Id")
    @Mapping(target = "address.adminArea3Id", source = "adminArea3Id")
    @Mapping(target = "address.localityName", ignore = true)
    @Mapping(target = "address.normalizedLocalityName", ignore = true)
    @Mapping(target = "address.adminArea1Name", ignore = true)
    @Mapping(target = "address.adminArea2Name", ignore = true)
    @Mapping(target = "address.adminArea3Name", ignore = true)
    @Mapping(target = "openingPeriods", source = "openingPeriods", qualifiedByName = "mapOpeningPeriodsToDomain")
    Station toDomain(StationEntity station);

    @Named("mapOpeningPeriodsToDomain")
    default List<OpeningPeriod> mapOpeningPeriodsToDomain(List<StationOpeningPeriodEntity> openingPeriods) {
        if (openingPeriods == null) {
            return List.of();
        }

        return openingPeriods.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        openingPeriod -> openingPeriod.getOpen() + "|" + openingPeriod.getClose()))
                .values()
                .stream()
                .map(group -> OpeningPeriod.builder()
                        .open(group.getFirst().getOpen())
                        .close(group.getFirst().getClose())
                        .days(group.stream()
                                .map(StationOpeningPeriodEntity::getDay)
                                .toList())
                        .build())
                .toList();
    }
}
