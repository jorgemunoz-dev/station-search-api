package com.petrolprice.station_search_api.station.search.infrastructure.rest.request;

import com.petrolprice.station_search_api.contract.rest.model.ProductType;
import com.petrolprice.station_search_api.contract.rest.model.StationSearchMode;
import com.petrolprice.station_search_api.contract.rest.model.StationSearchSortBy;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record StationSearchParameters(
        StationSearchMode searchMode,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer radiusMeters,
        Double north,
        Double south,
        Double east,
        Double west,
        ProductType productType,
        StationSearchSortBy sortBy,
        int page,
        int size) {}
