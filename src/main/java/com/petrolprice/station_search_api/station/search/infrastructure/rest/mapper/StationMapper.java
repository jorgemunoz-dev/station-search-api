package com.petrolprice.station_search_api.station.search.infrastructure.rest.mapper;

import com.petrolprice.station_search_api.contract.rest.model.*;
import com.petrolprice.station_search_api.station.domain.model.Address;
import com.petrolprice.station_search_api.station.domain.model.OpeningPeriod;
import com.petrolprice.station_search_api.station.domain.model.ProductPrice;
import com.petrolprice.station_search_api.station.search.application.result.FindStationsItem;
import com.petrolprice.station_search_api.station.search.application.result.FindStationsResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StationMapper {

    @Mapping(target = "stations", source = "items")
    StationSearchResponse toResponse(FindStationsResult result);

    @Mapping(target = "id", source = "station.id")
    @Mapping(target = "brand", source = "station.brand")
    @Mapping(target = "normalizedBrand", source = "station.normalizedBrand")
    @Mapping(target = "latitude", source = "station.location.latitude")
    @Mapping(target = "longitude", source = "station.location.longitude")
    @Mapping(target = "address", source = "station.address")
    @Mapping(target = "productPrices", source = "station.productPrices")
    @Mapping(target = "openingPeriods", source = "station.openingPeriods")
    StationSearchItemResponse toStationItemResponse(FindStationsItem station);

    AddressResponse toAddressResponse(Address address);

    ProductPriceResponse toProductPriceResponse(ProductPrice productPrice);

    OpeningPeriodResponse toOpeningPeriodResponse(OpeningPeriod openingPeriod);
}
