package com.petrolprice.station_search_api.infrastructure.in.rest;

import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsQuery;
import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsResult;
import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsUseCase;
import com.petrolprice.station_search_api.infrastructure.in.rest.api.StationsApi;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.ProductType;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.StationSearchResponse;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.StationSearchSortBy;
import com.petrolprice.station_search_api.infrastructure.in.rest.mapper.StationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StationController implements StationsApi {

    private final FindStationsUseCase findStationsUseCase;
    private final StationMapper mapper;

    @Override
    public ResponseEntity<StationSearchResponse> searchStations(
        Double lat,
        Double lng,
        Integer radiusMeters,
        ProductType productType,
        StationSearchSortBy sortBy,
        Integer limit
    ) {
        FindStationsQuery query = mapper.toQuery(
            lat,
            lng,
            radiusMeters,
            productType,
            sortBy,
            limit
        );
        FindStationsResult result = findStationsUseCase.find(query);
        StationSearchResponse response = mapper.toResponse(result);

        return ResponseEntity.ok(response);
    }
}
