package com.petrolprice.station_search_api.station.search.application.searcharea;

import java.util.Locale;

public record LocalitySearchArea(
        Long localityId, String countryCode, String normalizedAdminArea1, String normalizedAdminArea2,
        String normalizedAdminArea3) implements StationSearchArea {
    public LocalitySearchArea(String countryCode, String adminArea1, String adminArea2, String adminArea3) {
        this(null, countryCode, adminArea1, adminArea2, adminArea3);
    }

    public LocalitySearchArea {
        if (localityId != null && localityId <= 0) throw new IllegalArgumentException("localityId must be positive");
        if (countryCode == null || !countryCode.matches("[A-Za-z]{2}"))
            throw new IllegalArgumentException("countryCode must be a two-letter ISO country code");
        countryCode = countryCode.toUpperCase(Locale.ROOT);
        if (localityId == null) {
            normalizedAdminArea1 = required(normalizedAdminArea1, "adminArea1");
            normalizedAdminArea2 = required(normalizedAdminArea2, "adminArea2");
            normalizedAdminArea3 = required(normalizedAdminArea3, "adminArea3");
        }
    }
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return AdministrativeHierarchyNormalizer.normalize(value);
    }
}
