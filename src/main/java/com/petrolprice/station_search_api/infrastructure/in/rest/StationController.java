package com.petrolprice.station_search_api.infrastructure.in.rest;

import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsUseCase;
import com.petrolprice.station_search_api.application.usecase.findstations.query.FindStationsQuery;
import com.petrolprice.station_search_api.application.usecase.findstations.result.FindStationsResult;
import com.petrolprice.station_search_api.infrastructure.in.rest.api.StationsApi;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.ProductType;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.StationSearchMode;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.StationSearchResponse;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.StationSearchSortBy;
import com.petrolprice.station_search_api.infrastructure.in.rest.factory.FindStationsQueryFactory;
import com.petrolprice.station_search_api.infrastructure.in.rest.mapper.StationMapper;
import com.petrolprice.station_search_api.infrastructure.in.rest.request.StationSearchParameters;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StationController implements StationsApi {

    private final FindStationsUseCase findStationsUseCase;
    private final FindStationsQueryFactory queryFactory;
    private final StationMapper responseMapper;

    @Override
    public ResponseEntity<StationSearchResponse> searchStations(
            StationSearchMode searchMode,
            Integer page,
            Integer size,
            Double lat,
            Double lng,
            Integer radiusMeters,
            Double north,
            Double south,
            Double east,
            Double west,
            ProductType productType,
            StationSearchSortBy sortBy) {
        StationSearchParameters parameters = StationSearchParameters.builder()
                .searchMode(searchMode)
                .latitude(lat == null ? null : BigDecimal.valueOf(lat)) // TODO: fix this
                .longitude(lng == null ? null : BigDecimal.valueOf(lng))
                .radiusMeters(radiusMeters)
                .north(north)
                .south(south)
                .east(east)
                .west(west)
                .productType(productType)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        FindStationsQuery query = queryFactory.create(parameters);

        FindStationsResult result = findStationsUseCase.find(query);

        return ResponseEntity.ok(responseMapper.toResponse(result));
    }

}
