package com.petrolprice.station_search_api.application.usecase.findstations;

import com.petrolprice.station_search_api.domain.type.ProductType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record FindStationsQuery (
    BigDecimal latitude,
    BigDecimal longitude,
    Integer radiusMeters,
    ProductType productType,
    FindStationsSort sortBy,
    Integer limit
) {
    public FindStationsQuery {
        if (latitude == null) {
            throw new IllegalArgumentException("latitude is required");
        }

        if (longitude == null) {
            throw new IllegalArgumentException("longitude is required");
        }

        if (radiusMeters == null) {
            throw new IllegalArgumentException("radiusMeters is required");
        }

        if (radiusMeters <= 0) {
            throw new IllegalArgumentException("radiusMeters must be greater than 0");
        }

        if (sortBy == null) {
            sortBy = FindStationsSort.DISTANCE;
        }

        if (limit == null) {
            limit = 50;
        }

        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0");
        }
    }
}
