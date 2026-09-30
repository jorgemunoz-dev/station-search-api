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
    @Mapping(target = "localityName", source = "address.localityName")
    @Mapping(target = "normalizedLocalityName", source = "address.normalizedLocalityName")
    @Mapping(target = "adminArea1Name", source = "address.adminArea1Name")
    @Mapping(target = "adminArea2Name", source = "address.adminArea2Name")
    @Mapping(target = "adminArea3Name", source = "address.adminArea3Name")
    @Mapping(target = "openingPeriods", source = "station.openingPeriods", qualifiedByName = "mapOpeningPeriods")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StationEntity toEntity(Station station);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "street", source = "address.street")
    @Mapping(target = "postalCode", source = "address.postalCode")
    @Mapping(target = "localityName", source = "address.localityName")
    @Mapping(target = "normalizedLocalityName", source = "address.normalizedLocalityName")
    @Mapping(target = "adminArea1Name", source = "address.adminArea1Name")
    @Mapping(target = "adminArea2Name", source = "address.adminArea2Name")
    @Mapping(target = "adminArea3Name", source = "address.adminArea3Name")
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
    @Mapping(target = "address.localityName", source = "localityName")
    @Mapping(target = "address.normalizedLocalityName", source = "normalizedLocalityName")
    @Mapping(target = "address.adminArea1Name", source = "adminArea1Name")
    @Mapping(target = "address.adminArea2Name", source = "adminArea2Name")
    @Mapping(target = "address.adminArea3Name", source = "adminArea3Name")
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
