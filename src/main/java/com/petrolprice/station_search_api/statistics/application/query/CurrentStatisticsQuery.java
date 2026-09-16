package com.petrolprice.station_search_api.statistics.application.query;

import com.petrolprice.station_search_api.statistics.domain.ProductType;

public record CurrentStatisticsQuery(String countryCode, ProductType productType, GeographicScope scope) {
    public CurrentStatisticsQuery {
        if (countryCode == null || countryCode.length() != 2) {
            throw new IllegalArgumentException("countryCode must have two characters");
        }
        if (productType == null || scope == null) {
            throw new IllegalArgumentException("productType and scope are required");
        }
        countryCode = countryCode.toUpperCase();
    }
}
