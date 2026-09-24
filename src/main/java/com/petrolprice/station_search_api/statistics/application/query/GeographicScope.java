package com.petrolprice.station_search_api.statistics.application.query;

import java.text.Normalizer;
import java.util.Locale;

public record GeographicScope(
        String normalizedLocalityName,
        String localityName,
        String adminArea1Name,
        String adminArea2Name,
        String adminArea3Name) {

    public static GeographicScope country() {
        return new GeographicScope(null, null, null, null, null);
    }

    public static GeographicScope locality(String localityName) {
        if (localityName == null || localityName.isBlank()) {
            throw new IllegalArgumentException("locality is required");
        }
        String normalized = Normalizer.normalize(localityName, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
        return new GeographicScope(normalized, null, null, null, null);
    }

    public static GeographicScope resolvedLocality(
            String normalizedName,
            String name,
            String adminArea1Name,
            String adminArea2Name,
            String adminArea3Name) {
        return new GeographicScope(normalizedName, name, adminArea1Name, adminArea2Name, adminArea3Name);
    }
}
