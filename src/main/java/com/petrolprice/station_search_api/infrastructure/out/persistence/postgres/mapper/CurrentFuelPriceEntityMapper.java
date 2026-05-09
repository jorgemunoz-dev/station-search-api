package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper;

import com.petrolprice.station_search_api.domain.model.FuelPrice;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.CurrentFuelPriceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CurrentFuelPriceEntityMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CurrentFuelPriceEntity toEntity(FuelPrice fuelPrice);
}
