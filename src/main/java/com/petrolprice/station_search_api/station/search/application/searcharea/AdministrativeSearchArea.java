package com.petrolprice.station_search_api.station.search.application.searcharea;

import java.util.Locale;

public record AdministrativeSearchArea(Type type, String name, String countryCode) implements StationSearchArea {

    public AdministrativeSearchArea {
        if (type == null) {
            throw new IllegalArgumentException("Administrative area type is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Administrative area name is required");
        }
        if (countryCode == null || !countryCode.matches("[A-Za-z]{2}")) {
            throw new IllegalArgumentException("countryCode must contain two letters");
        }

        name = name.trim();
        countryCode = countryCode.toUpperCase(Locale.ROOT);
    }

    public enum Type {
        LOCALITY,
        MUNICIPALITY,
        PROVINCE
    }
}
