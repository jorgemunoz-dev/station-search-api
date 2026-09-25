package com.petrolprice.station_search_api.location.infrastructure.postgres.projection;

public record SearchLocationProjection(
        String suggestionType,
        String primaryText,
        String secondaryText,
        String countryCode,
        String postalCode,
        String normalizedLocalityName,
        String adminArea1Name,
        String adminArea1Code,
        String adminArea2Name,
        String adminArea2Code,
        String adminArea3Name,
        String adminArea3Code,
        double latitude,
        double longitude) {}
