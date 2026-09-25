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

    public static GeographicScope adminArea1(String name) {
        return administrativeArea(name, 1);
    }

    public static GeographicScope adminArea2(String name) {
        return administrativeArea(name, 2);
    }

    public static GeographicScope adminArea3(String name) {
        return administrativeArea(name, 3);
    }

    public static GeographicScope resolvedLocality(
            String normalizedName,
            String name,
            String adminArea1Name,
            String adminArea2Name,
            String adminArea3Name) {
        return new GeographicScope(normalizedName, name, adminArea1Name, adminArea2Name, adminArea3Name);
    }

    private static GeographicScope administrativeArea(String name, int position) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("adminArea" + position + " is required");
        }
        String trimmedName = name.trim();
        return switch (position) {
            case 1 -> new GeographicScope(null, null, trimmedName, null, null);
            case 2 -> new GeographicScope(null, null, null, trimmedName, null);
            case 3 -> new GeographicScope(null, null, null, null, trimmedName);
            default -> throw new IllegalArgumentException("Unsupported administrative area position");
        };
    }
}
