package com.petrolprice.station_search_api.location.infrastructure.postgres.projection;

public record SearchLocationProjection(
        String suggestionType,
        String primaryText,
        String secondaryText,
        String countryCode,
        String postalCode,
        double latitude,
        double longitude) {}
