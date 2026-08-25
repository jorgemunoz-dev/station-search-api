package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.mapper;

import com.petrolprice.station_search_api.domain.model.ProductPrice;
import com.petrolprice.station_search_api.domain.model.Station;
import com.petrolprice.station_search_api.domain.type.ProductType;
import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto.FuelPriceMessage;
import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto.StationSnapshotMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StationSnapshotMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productPrices", source = "fuelPrices")
    Station toModel(StationSnapshotMessage message);

    ProductPrice toModel(FuelPriceMessage message);

    default ProductType map(String stationProductType) {
        return ProductType.valueOf(stationProductType);
    }
}
