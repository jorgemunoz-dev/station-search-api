package com.petrolprice.station_search_api.infrastructure.in.rest;

import com.petrolprice.station_search_api.infrastructure.in.rest.api.StatisticsApi;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.FuelPriceSummaryResponse;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.ProductType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FuelPriceStatisticsController implements StatisticsApi {
    @Override
    public ResponseEntity<FuelPriceSummaryResponse> getFuelPriceSummary(String countryCode, ProductType productType, Integer days) {
        return null;
    }
}
