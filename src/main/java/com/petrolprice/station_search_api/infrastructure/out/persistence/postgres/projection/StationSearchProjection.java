package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.projection;

import java.math.BigDecimal;
import java.util.UUID;

public record StationSearchProjection(
        UUID stationId,
        String externalId,
        String country,
        String brand,
        String street,
        String postalCode,
        String locality,
        String municipality,
        String province,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal distanceMeters,
        String productType,
        BigDecimal price) {}
