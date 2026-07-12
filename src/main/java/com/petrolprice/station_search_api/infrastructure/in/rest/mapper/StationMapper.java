package com.petrolprice.station_search_api.infrastructure.in.rest.mapper;

import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsItem;
import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsQuery;
import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsResult;
import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsSort;
import com.petrolprice.station_search_api.domain.model.Address;
import com.petrolprice.station_search_api.domain.model.OpeningPeriod;
import com.petrolprice.station_search_api.domain.model.ProductPrice;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface StationMapper {

    default FindStationsQuery toQuery(
        Double lat,
        Double lng,
        Integer radiusMeters,
        ProductType productType,
        StationSearchSortBy sortBy,
        Integer limit
    ) {
        return FindStationsQuery.builder()
            .latitude(BigDecimal.valueOf(lat))
            .longitude(BigDecimal.valueOf(lng))
            .radiusMeters(radiusMeters)
            .productType(toDomainProductType(productType))
            .sortBy(toFindStationsSort(sortBy))
            .limit(limit != null ? limit : 50)
            .build();
    }

    default com.petrolprice.station_search_api.domain.type.ProductType toDomainProductType(ProductType productType) {
        if (productType == null) {
            return null;
        }

        return com.petrolprice.station_search_api.domain.type.ProductType.valueOf(productType.name());
    }

    default FindStationsSort toFindStationsSort(StationSearchSortBy sortBy) {
        if (sortBy == null) {
            return FindStationsSort.DISTANCE;
        }

        return FindStationsSort.valueOf(sortBy.name());
    }


    StationSearchResponse toResponse(FindStationsResult result);

    @Mapping(target = "id", source = "station.id")
    @Mapping(target = "brand", source = "station.brand")
    @Mapping(target = "latitude", source = "station.location.latitude")
    @Mapping(target = "longitude", source = "station.location.longitude")
    @Mapping(target = "address", source = "station.address")
    @Mapping(target = "productPrices", source = "station.productPrices")
    @Mapping(target = "openingPeriods", source = "station.openingPeriods")
    StationSearchItemResponse toStationItemResponse(FindStationsItem station);

    AddressResponse toAddressResponse(Address address);

    ProductPriceResponse toProductPriceResponse(ProductPrice productPrice);

    OpeningPeriodResponse toOpeningPeriod(OpeningPeriod openingPeriod);
}
