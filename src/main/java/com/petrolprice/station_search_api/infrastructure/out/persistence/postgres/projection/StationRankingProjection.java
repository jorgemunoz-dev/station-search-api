package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.projection;

import java.util.UUID;

public record StationRankingProjection(
    UUID id,
    String externalId,
    String country,
    String brand,
    String street,
    String postalCode,
    String locality,
    String municipality,
    String province,
    double latitude,
    double longitude,
    Double distanceMeters
) {
}