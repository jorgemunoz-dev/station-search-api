package com.petrolprice.station_search_api.statistics.application.result;

import java.math.BigDecimal;
import java.util.UUID;

public record RankedAreaStatistics(
        UUID areaId,
        String area,
        String areaType,
        UUID parentAreaId,
        BigDecimal averagePrice,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice,
        long stationCount,
        StationPricePoint cheapestStation,
        BigDecimal nationalAverageDifference,
        int cheapestRank,
        int mostExpensiveRank) {}
