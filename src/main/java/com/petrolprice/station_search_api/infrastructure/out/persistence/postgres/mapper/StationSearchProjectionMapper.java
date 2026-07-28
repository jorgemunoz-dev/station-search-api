package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper;

import com.petrolprice.station_search_api.application.usecase.findstations.result.FindStationsItem;
import com.petrolprice.station_search_api.domain.model.*;
import com.petrolprice.station_search_api.domain.type.Country;
import com.petrolprice.station_search_api.domain.type.Day;
import com.petrolprice.station_search_api.domain.type.ProductType;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.projection.OpeningPeriodProjection;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.projection.ProductPriceProjection;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.projection.StationRankingProjection;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StationSearchProjectionMapper {

    default FindStationsItem toItem(
            StationRankingProjection projection, List<ProductPrice> productPrices, List<OpeningPeriod> openingPeriods) {
        Station station = Station.builder()
                .id(projection.id())
                .externalId(projection.externalId())
                .country(toCountry(projection.country()))
                .brand(projection.brand())
                .address(toAddress(projection))
                .location(toGeoLocation(projection))
                .productPrices(productPrices)
                .openingPeriods(openingPeriods)
                .build();

        return new FindStationsItem(
                station,
                projection.distanceMeters() == null
                        ? null
                        : BigDecimal.valueOf(projection.distanceMeters()) // TODO: fix this
                );
    }

    default OpeningPeriod toOpeningPeriod(List<OpeningPeriodProjection> projections) {
        OpeningPeriodProjection first = projections.getFirst();

        return OpeningPeriod.builder()
                .days(projections.stream().map(p -> Day.valueOf(p.dayOfWeek())).toList())
                .open(first.openTime())
                .close(first.closeTime())
                .build();
    }

    default Address toAddress(StationRankingProjection projection) {
        return Address.builder()
                .street(projection.street())
                .postalCode(projection.postalCode())
                .locality(projection.locality())
                .municipality(projection.municipality())
                .province(projection.province())
                .build();
    }

    default GeoLocation toGeoLocation(StationRankingProjection projection) {
        return GeoLocation.builder()
                .latitude(BigDecimal.valueOf(projection.latitude()))
                .longitude(BigDecimal.valueOf(projection.longitude()))
                .build();
    }

    default ProductPrice toProductPrice(ProductPriceProjection projection) {
        if (projection.productType() == null || projection.price() == null) {
            return null;
        }

        return ProductPrice.builder()
                .productType(ProductType.valueOf(projection.productType()))
                .price(projection.price())
                .build();
    }

    default Country toCountry(String country) {
        return country == null ? null : Country.valueOf(country);
    }
}
