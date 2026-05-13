package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper;

import com.petrolprice.station_search_api.domain.model.ProductPrice;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.HistoricalFuelPriceEntity;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HistoricalFuelPriceEntityMapper {
    HistoricalFuelPriceEntity toEntity(UUID stationId, ProductPrice productPrice);
}
