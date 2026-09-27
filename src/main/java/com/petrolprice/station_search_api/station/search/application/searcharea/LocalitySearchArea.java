package com.petrolprice.station_search_api.station.search.application.searcharea;

import java.text.Normalizer;
import java.util.Locale;

public record LocalitySearchArea(
        String countryCode, String normalizedAdminArea1, String normalizedAdminArea2, String normalizedAdminArea3)
        implements StationSearchArea {

    public LocalitySearchArea {
        countryCode = validateCountryCode(countryCode);
        normalizedAdminArea1 = normalizeAdministrativeArea(normalizedAdminArea1, "adminArea1");
        normalizedAdminArea2 = normalizeAdministrativeArea(normalizedAdminArea2, "adminArea2");
        normalizedAdminArea3 = normalizeAdministrativeArea(normalizedAdminArea3, "adminArea3");
    }

    private static String validateCountryCode(String countryCode) {
        if (countryCode == null || !countryCode.matches("[A-Za-z]{2}")) {
            throw new IllegalArgumentException("countryCode must be a two-letter ISO country code");
        }
        return countryCode.toUpperCase(Locale.ROOT);
    }

    private static String normalizeAdministrativeArea(String value, String parameter) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(parameter + " is required");
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
