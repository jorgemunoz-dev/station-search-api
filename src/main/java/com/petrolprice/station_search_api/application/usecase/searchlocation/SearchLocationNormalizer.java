package com.petrolprice.station_search_api.application.usecase.searchlocation;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SearchLocationNormalizer {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");

    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

    private static final Pattern POSTAL_SEPARATORS = Pattern.compile("[^A-Z0-9]");

    public String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD);

        normalized = DIACRITICS.matcher(normalized).replaceAll("");

        normalized = normalized.toLowerCase(Locale.ROOT);

        normalized = NON_ALPHANUMERIC.matcher(normalized).replaceAll(" ");

        return MULTIPLE_SPACES.matcher(normalized).replaceAll(" ").trim();
    }

    public String normalizePostalCode(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return POSTAL_SEPARATORS.matcher(value.trim().toUpperCase(Locale.ROOT)).replaceAll("");
    }
}
