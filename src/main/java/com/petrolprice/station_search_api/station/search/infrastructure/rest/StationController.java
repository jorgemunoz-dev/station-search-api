package com.petrolprice.station_search_api.station.search.infrastructure.rest;

import com.petrolprice.station_search_api.contract.rest.api.StationsApi;
import com.petrolprice.station_search_api.contract.rest.model.AdministrativeAreaType;
import com.petrolprice.station_search_api.contract.rest.model.ProductType;
import com.petrolprice.station_search_api.contract.rest.model.StationSearchMode;
import com.petrolprice.station_search_api.contract.rest.model.StationSearchResponse;
import com.petrolprice.station_search_api.contract.rest.model.StationSearchSortBy;
import com.petrolprice.station_search_api.station.search.application.FindStationsUseCase;
import com.petrolprice.station_search_api.station.search.application.query.FindStationsQuery;
import com.petrolprice.station_search_api.station.search.application.result.FindStationsResult;
import com.petrolprice.station_search_api.station.search.infrastructure.rest.factory.FindStationsQueryFactory;
import com.petrolprice.station_search_api.station.search.infrastructure.rest.mapper.StationMapper;
import com.petrolprice.station_search_api.station.search.infrastructure.rest.request.StationSearchParameters;
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

    @Override
    public ResponseEntity<StationSearchResponse> searchStationsByAdministrativeArea(
            AdministrativeAreaType areaType,
            String name,
            String countryCode,
            Integer page,
            Integer size,
            ProductType productType) {
        FindStationsQuery query = queryFactory.createAdministrativeArea(
                areaType, name, countryCode, productType, page, size);

        FindStationsResult result = findStationsUseCase.find(query);

        return ResponseEntity.ok(responseMapper.toResponse(result));
    }
}
