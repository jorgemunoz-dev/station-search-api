package com.petrolprice.station_search_api.statistics.application.query;

import com.petrolprice.station_search_api.statistics.domain.ProductType;
import java.time.LocalDate;

public record HistoricalStatisticsQuery(
        String countryCode, ProductType productType, GeographicScope scope, LocalDate from, LocalDate to) {
    public HistoricalStatisticsQuery {
        if (countryCode == null || countryCode.length() != 2 || productType == null || scope == null) {
            throw new IllegalArgumentException("countryCode, productType and scope are required");
        }
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("A valid historical date range is required");
        }
        countryCode = countryCode.toUpperCase();
    }
}
