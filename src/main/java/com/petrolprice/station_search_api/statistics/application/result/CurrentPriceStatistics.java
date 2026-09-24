package com.petrolprice.station_search_api.statistics.application.result;

import com.petrolprice.station_search_api.statistics.application.query.GeographicScope;
import com.petrolprice.station_search_api.statistics.domain.ProductType;
import java.math.BigDecimal;
import java.time.Instant;

public record CurrentPriceStatistics(
        String countryCode,
        ProductType productType,
        GeographicScope scope,
        BigDecimal averagePrice,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice,
        long stationCount,
        StationPricePoint cheapestStation,
        StationPricePoint mostExpensiveStation,
        BigDecimal countryAverageDifference,
        BigDecimal parentAreaAverageDifference,
        Instant updatedAt) {}
