package com.petrolprice.station_search_api.statistics.infrastructure.rest;

import com.petrolprice.station_search_api.contract.rest.api.AdministrativeAreasApi;
import com.petrolprice.station_search_api.contract.rest.model.AdministrativeAreaResponse;
import com.petrolprice.station_search_api.statistics.application.FuelPriceStatisticsUseCase;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdministrativeAreaController implements AdministrativeAreasApi {
    private final FuelPriceStatisticsUseCase statistics;
    private final StatisticsRestMapper mapper;

    @Override
    public ResponseEntity<List<AdministrativeAreaResponse>> getAdministrativeAreas(
            String countryCode, UUID parentAreaId, String areaType) {
        return ResponseEntity.ok(mapper.toAreaResponse(statistics.findAreas(countryCode, parentAreaId, areaType)));
    }
}
