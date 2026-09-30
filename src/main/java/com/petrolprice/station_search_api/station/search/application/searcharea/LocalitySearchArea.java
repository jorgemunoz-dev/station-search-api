package com.petrolprice.station_search_api.station.search.application.searcharea;

import java.util.Locale;

public record LocalitySearchArea(
        String countryCode, String normalizedAdminArea1, String normalizedAdminArea2,
        String normalizedAdminArea3) implements StationSearchArea {
    public LocalitySearchArea {
        if (countryCode == null || !countryCode.matches("[A-Za-z]{2}"))
            throw new IllegalArgumentException("countryCode must be a two-letter ISO country code");
        countryCode = countryCode.toUpperCase(Locale.ROOT);
        normalizedAdminArea1 = required(normalizedAdminArea1, "adminArea1");
        normalizedAdminArea2 = required(normalizedAdminArea2, "adminArea2");
        normalizedAdminArea3 = required(normalizedAdminArea3, "adminArea3");
    }
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return AdministrativeHierarchyNormalizer.normalize(value);
    }
}
