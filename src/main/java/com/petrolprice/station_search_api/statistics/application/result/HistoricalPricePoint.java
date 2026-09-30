package com.petrolprice.station_search_api.statistics.application.result;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HistoricalPricePoint(
        LocalDate date,
        BigDecimal averagePrice,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice,
        long stationCount,
        BigDecimal periodAveragePrice,
        BigDecimal periodMinimumPrice,
        BigDecimal periodMaximumPrice,
        BigDecimal changeFromPreviousDay,
        BigDecimal changeFromSevenDaysAgo,
        BigDecimal changeFromThirtyDaysAgo,
        BigDecimal percentageFromPreviousDay,
        BigDecimal percentageFromSevenDaysAgo,
        BigDecimal percentageFromThirtyDaysAgo) {}
