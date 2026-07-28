package com.petrolprice.station_search_api.application.usecase.findstations.searcharea;

import java.math.BigDecimal;
import java.util.Objects;

public record RadiusSearchArea(BigDecimal latitude, BigDecimal longitude, int radiusMeters)
        implements StationSearchArea {

    public RadiusSearchArea {
        Objects.requireNonNull(latitude, "latitude is required");
        Objects.requireNonNull(longitude, "longitude is required");

        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }

        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }

        if (radiusMeters <= 0) {
            throw new IllegalArgumentException("radiusMeters must be greater than 0");
        }

        if (radiusMeters > 50_000) {
            throw new IllegalArgumentException("radiusMeters must not be greater than 50000");
        }
    }
}
