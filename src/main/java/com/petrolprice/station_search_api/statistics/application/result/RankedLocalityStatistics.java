package com.petrolprice.station_search_api.statistics.application.result;

import java.math.BigDecimal;

public record RankedLocalityStatistics(
        String normalizedLocalityName,
        String localityName,
        String adminArea1Name,
        String adminArea2Name,
        String adminArea3Name,
        BigDecimal averagePrice,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice,
        long stationCount,
        StationPricePoint cheapestStation,
        BigDecimal countryAverageDifference,
        int cheapestRank,
        int mostExpensiveRank) {}
