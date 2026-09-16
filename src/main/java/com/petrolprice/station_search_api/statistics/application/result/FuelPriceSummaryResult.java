package com.petrolprice.station_search_api.statistics.application.result;

import com.petrolprice.station_search_api.statistics.domain.ProductType;

import java.time.Instant;

public record FuelPriceSummaryResult(
    String countryCode,
    ProductType productType,
    double averagePrice,
    Double previousAveragePrice,
    Double variation,
    Double variationPercentage,
    Double minimumPrice,
    Double maximumPrice,
    long stationsAnalyzed,
    EstimatedSaving estimatedSaving,
    Instant updatedAt
) {
}
