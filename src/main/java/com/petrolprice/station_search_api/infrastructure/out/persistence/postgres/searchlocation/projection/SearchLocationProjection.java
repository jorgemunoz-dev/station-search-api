package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.searchlocation.projection;

public record SearchLocationProjection(
        String suggestionType,
        String primaryText,
        String secondaryText,
        String countryCode,
        String postalCode,
        double latitude,
        double longitude) {}
