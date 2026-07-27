package com.petrolprice.station_search_api.infrastructure.in.rest.mapper;

import com.petrolprice.station_search_api.application.usecase.findstations.result.FindStationsItem;
import com.petrolprice.station_search_api.application.usecase.findstations.result.FindStationsResult;
import com.petrolprice.station_search_api.domain.model.Address;
import com.petrolprice.station_search_api.domain.model.OpeningPeriod;
import com.petrolprice.station_search_api.domain.model.ProductPrice;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StationMapper {


    @Mapping(target = "stations", source = "items")
    StationSearchResponse toResponse(FindStationsResult result);

    @Mapping(target = "id", source = "station.id")
    @Mapping(target = "brand", source = "station.brand")
    @Mapping(target = "latitude", source = "station.location.latitude")
    @Mapping(target = "longitude", source = "station.location.longitude")
    @Mapping(target = "address", source = "station.address")
    @Mapping(
        target = "productPrices",
        source = "station.productPrices"
    )
    @Mapping(
        target = "openingPeriods",
        source = "station.openingPeriods"
    )
    StationSearchItemResponse toStationItemResponse(
        FindStationsItem station
    );

    AddressResponse toAddressResponse(Address address);

    ProductPriceResponse toProductPriceResponse(
        ProductPrice productPrice
    );

    OpeningPeriodResponse toOpeningPeriodResponse(
        OpeningPeriod openingPeriod
    );
}
