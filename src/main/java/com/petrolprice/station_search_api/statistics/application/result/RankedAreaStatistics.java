package com.petrolprice.station_search_api.statistics.application.result;

import java.math.BigDecimal;

public record RankedAreaStatistics(
        String area,
        BigDecimal averagePrice,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice,
        long stationCount,
        StationPricePoint cheapestStation,
        BigDecimal nationalAverageDifference,
        int cheapestRank,
        int mostExpensiveRank) {}
