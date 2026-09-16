package com.petrolprice.station_search_api.statistics.application.result;

import java.math.BigDecimal;
import java.util.UUID;

public record StationPricePoint(
        UUID stationId,
        String externalId,
        String brand,
        BigDecimal price,
        BigDecimal latitude,
        BigDecimal longitude,
        Double distanceMeters) {}
