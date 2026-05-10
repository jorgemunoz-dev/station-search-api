package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper;

import com.petrolprice.station_search_api.domain.model.OpeningPeriod;
import com.petrolprice.station_search_api.domain.type.Day;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.StationOpeningPeriodEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StationOpeningPeriodEntityMapper {
    default List<StationOpeningPeriodEntity> toEntities(OpeningPeriod openingPeriod) {
        if (openingPeriod == null || openingPeriod.getDays() == null) {
            return List.of();
        }

        return openingPeriod.getDays().stream()
                .map(day -> toEntity(openingPeriod, day))
                .toList();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "day", source = "day")
    @Mapping(target = "open", source = "openingPeriod.open")
    @Mapping(target = "close", source = "openingPeriod.close")
    StationOpeningPeriodEntity toEntity(OpeningPeriod openingPeriod, Day day);
}
