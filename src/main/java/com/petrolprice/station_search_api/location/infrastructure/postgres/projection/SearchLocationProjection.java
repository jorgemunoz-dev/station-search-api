package com.petrolprice.station_search_api.location.infrastructure.postgres.projection;

public record SearchLocationProjection(
        String suggestionType,
        String primaryText,
        String secondaryText,
        String countryCode,
        String postalCode,
        long localityId,
        String stationLocalityName,
        String normalizedLocalityName,
        Long adminArea1Id,
        String adminArea1Name,
        String adminArea1Code,
        Long adminArea2Id,
        String adminArea2Name,
        String adminArea2Code,
        Long adminArea3Id,
        String adminArea3Name,
        String adminArea3Code,
        double latitude,
        double longitude) {}
