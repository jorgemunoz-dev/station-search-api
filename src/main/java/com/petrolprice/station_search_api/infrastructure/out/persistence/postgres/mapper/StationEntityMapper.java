package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper;

import com.petrolprice.station_search_api.domain.model.OpeningPeriod;
import com.petrolprice.station_search_api.domain.model.Station;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.CurrentFuelPriceEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.StationEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.StationOpeningPeriodEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(
    componentModel = "spring",
    uses = {
        GeoLocationEntityMapper.class,
        CurrentFuelPriceEntityMapper.class
    }
)
public interface StationEntityMapper {

    @Mapping(target = "street", source = "address.street")
    @Mapping(target = "postalCode", source = "address.postalCode")
    @Mapping(target = "locality", source = "address.locality")
    @Mapping(target = "municipality", source = "address.municipality")
    @Mapping(target = "province", source = "address.province")
    @Mapping(target = "currentFuelPrices", source = "fuelPrices")
    @Mapping(target = "openingPeriods", source = "station.openingPeriods", qualifiedByName = "mapOpeningPeriods")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StationEntity toEntity(Station station);

    @Named("mapOpeningPeriods")
    default List<StationOpeningPeriodEntity> mapOpeningPeriods(List<OpeningPeriod> openingPeriods) {
        if (openingPeriods == null) {
            return List.of();
        }

        return openingPeriods.stream()
            .filter(openingPeriod -> openingPeriod.getDays() != null)
            .flatMap(openingPeriod ->
                openingPeriod.getDays().stream()
                    .map(day -> StationOpeningPeriodEntity.builder()
                        .day(day)
                        .open(openingPeriod.getOpen())
                        .close(openingPeriod.getClose())
                        .build()
                    )
            )
            .toList();
    }

}
