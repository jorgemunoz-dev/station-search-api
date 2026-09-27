package com.petrolprice.station_search_api.station.search.application.searcharea;

import java.text.Normalizer;
import java.util.Locale;

public record LocalitySearchArea(
        String countryCode,
        String normalizedLocality,
        String adminArea1Name,
        String adminArea2Name,
        String adminArea3Name)
        implements StationSearchArea {

    public LocalitySearchArea {
        countryCode = validateCountryCode(countryCode);
        normalizedLocality = normalizeLocality(normalizedLocality);
        adminArea1Name = requireAdministrativeArea(adminArea1Name, "adminArea1");
        adminArea2Name = requireAdministrativeArea(adminArea2Name, "adminArea2");
    }

    private static String validateCountryCode(String countryCode) {
        if (countryCode == null || !countryCode.matches("[A-Za-z]{2}")) {
            throw new IllegalArgumentException("countryCode must be a two-letter ISO country code");
        }
        return countryCode.toUpperCase(Locale.ROOT);
    }

    private static String normalizeLocality(String locality) {
        if (locality == null || locality.isBlank()) {
            throw new IllegalArgumentException("locality is required");
        }
        return Normalizer.normalize(locality, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String requireAdministrativeArea(String value, String parameter) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(parameter + " is required");
        }
        return value.trim();
    }
}
