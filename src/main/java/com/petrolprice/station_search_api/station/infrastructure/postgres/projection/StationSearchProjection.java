package com.petrolprice.station_search_api.station.infrastructure.postgres.projection;

import java.math.BigDecimal;
import java.util.UUID;

public record StationSearchProjection(
        UUID stationId,
        String externalId,
        String country,
        String brand,
        String normalizedBrand,
        String street,
        String postalCode,
        String localityName,
        String normalizedLocalityName,
        String adminArea1Name,
        String adminArea2Name,
        String adminArea3Name,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal distanceMeters,
        String productType,
        BigDecimal price) {}
