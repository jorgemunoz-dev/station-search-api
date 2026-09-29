package com.petrolprice.station_search_api.station.search.application.searcharea;

import java.text.Normalizer;
import java.util.Locale;

public final class AdministrativeHierarchyNormalizer {
    private AdministrativeHierarchyNormalizer() {}

    public static String normalize(String value) {
        if (value == null) return null;
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ").trim().replaceAll("\\s+", " ");
    }
}
