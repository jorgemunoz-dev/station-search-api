package com.petrolprice.station_search_api.infrastructure.in.rest.request;

import com.petrolprice.station_search_api.infrastructure.in.rest.dto.ProductType;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.StationSearchMode;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.StationSearchSortBy;
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
