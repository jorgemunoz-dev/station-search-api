package com.petrolprice.station_search_api.application.usecase.findstations.query;

import com.petrolprice.station_search_api.application.usecase.findstations.searcharea.StationSearchArea;
import com.petrolprice.station_search_api.domain.type.ProductType;
import lombok.Builder;

import java.util.Objects;

@Builder
public record FindStationsQuery(
    StationSearchArea searchArea,
    ProductType productType,
    FindStationsSort sortBy,
    FindStationsPageRequest pageRequest
) {
    public FindStationsQuery {
        Objects.requireNonNull(
            searchArea,
            "Search area is required"
        );

        Objects.requireNonNull(
            sortBy,
            "Sort is required"
        );

        Objects.requireNonNull(
            pageRequest,
            "Page request is required"
        );
    }
}
