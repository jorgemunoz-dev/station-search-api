package com.petrolprice.station_search_api.application.usecase.statistics.result;

import com.petrolprice.station_search_api.domain.type.ProductType;

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
