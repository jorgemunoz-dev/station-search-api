package com.petrolprice.station_search_api.station.infrastructure.postgres.mapper;

import com.petrolprice.station_search_api.station.domain.model.ProductPrice;
import com.petrolprice.station_search_api.station.infrastructure.postgres.entity.CurrentFuelPriceEntity;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CurrentPriceEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CurrentFuelPriceEntity toEntity(UUID stationId, ProductPrice price);
}
