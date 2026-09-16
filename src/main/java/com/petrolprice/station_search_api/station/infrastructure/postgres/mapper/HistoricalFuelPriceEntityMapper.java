package com.petrolprice.station_search_api.station.infrastructure.postgres.mapper;

import com.petrolprice.station_search_api.station.domain.model.ProductPrice;
import com.petrolprice.station_search_api.station.infrastructure.postgres.entity.HistoricalFuelPriceEntity;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HistoricalFuelPriceEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "observedAt", ignore = true)
    @Mapping(target = "snapshotId", source = "snapshotId")
    @Mapping(target = "stationId", source = "stationId")
    @Mapping(target = "productType", source = "productPrice.productType")
    @Mapping(target = "price", source = "productPrice.price")
    HistoricalFuelPriceEntity toEntity(UUID snapshotId, UUID stationId, ProductPrice productPrice);
}
