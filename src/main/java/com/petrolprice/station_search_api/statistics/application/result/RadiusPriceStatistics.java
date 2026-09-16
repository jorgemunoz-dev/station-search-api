package com.petrolprice.station_search_api.statistics.application.result;

import com.petrolprice.station_search_api.statistics.domain.ProductType;
import java.math.BigDecimal;

public record RadiusPriceStatistics(
        String countryCode,
        ProductType productType,
        BigDecimal latitude,
        BigDecimal longitude,
        int radiusMeters,
        long stationCount,
        BigDecimal averagePrice,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice,
        BigDecimal priceSpread,
        StationPricePoint cheapestStation,
        StationPricePoint nearestStation) {}
