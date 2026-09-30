package com.petrolprice.station_search_api.statistics.application.query;

import com.petrolprice.station_search_api.statistics.domain.ProductType;
import java.math.BigDecimal;

public record RadiusStatisticsQuery(
        String countryCode, ProductType productType, BigDecimal latitude, BigDecimal longitude, int radiusMeters) {
    public RadiusStatisticsQuery {
        if (countryCode == null || countryCode.length() != 2 || productType == null) {
            throw new IllegalArgumentException("countryCode and productType are required");
        }
        if (latitude == null
                || longitude == null
                || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException("Valid coordinates are required");
        }
        if (radiusMeters != 5_000 && radiusMeters != 10_000 && radiusMeters != 20_000 && radiusMeters != 50_000) {
            throw new IllegalArgumentException("Radius must be 5000, 10000, 20000 or 50000 meters");
        }
        countryCode = countryCode.toUpperCase();
    }
}
