package com.petrolprice.station_search_api.statistics.infrastructure.rest;

import com.petrolprice.station_search_api.contract.rest.api.StatisticsApi;
import com.petrolprice.station_search_api.contract.rest.model.FuelPriceSummaryResponse;
import com.petrolprice.station_search_api.contract.rest.model.ProductType;
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
