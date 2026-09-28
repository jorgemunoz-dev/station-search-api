package com.petrolprice.station_search_api.station.infrastructure.postgres.projection;

import java.util.UUID;

public record StationRankingProjection(
        UUID id,
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
        double latitude,
        double longitude,
        Double distanceMeters) {}
