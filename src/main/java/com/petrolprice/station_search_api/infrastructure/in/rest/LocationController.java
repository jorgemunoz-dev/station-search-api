package com.petrolprice.station_search_api.infrastructure.in.rest;

import com.petrolprice.station_search_api.application.usecase.searchlocation.SearchLocationsUseCase;
import com.petrolprice.station_search_api.application.usecase.searchlocation.query.SearchLocationQuery;
import com.petrolprice.station_search_api.infrastructure.in.rest.api.LocationsApi;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.LocationSuggestion;
import com.petrolprice.station_search_api.infrastructure.in.rest.mapper.LocationMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LocationController implements LocationsApi {

    private final SearchLocationsUseCase searchLocationsUseCase;
    private final LocationMapper locationMapper;

    @Override
    public ResponseEntity<List<LocationSuggestion>> searchLocations(String query, String countryCode, Integer limit) {
        SearchLocationQuery searchQuery = new SearchLocationQuery(query, countryCode, limit);

        List<LocationSuggestion> response = searchLocationsUseCase.search(searchQuery).stream()
                .map(locationMapper::toDto)
                .toList();

        return ResponseEntity.ok(response);
    }
}
