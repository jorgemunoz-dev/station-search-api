package com.petrolprice.station_search_api.application.usecase.searchlocation.query;

public record SearchLocationQuery(String query, String countryCode, int limit) {
    public SearchLocationQuery {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        if (countryCode == null || countryCode.length() != 2) {
            throw new IllegalArgumentException("Country code must contain exactly two characters");
        }

        if (limit < 1 || limit > 20) {
            throw new IllegalArgumentException("Limit must be between 1 and 20");
        }

        query = query.trim();
        countryCode = countryCode.trim().toUpperCase();
    }
}
