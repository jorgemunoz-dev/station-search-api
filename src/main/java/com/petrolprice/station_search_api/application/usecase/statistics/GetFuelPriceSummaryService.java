package com.petrolprice.station_search_api.application.usecase.statistics;

import com.petrolprice.station_search_api.application.usecase.statistics.result.FuelPriceSummaryResult;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.ProductType;
import org.springframework.stereotype.Service;

@Service
public class GetFuelPriceSummaryService implements GetFuelPriceSummaryUseCase {

    @Override
    public FuelPriceSummaryResult getFuelPriceSummary(String countryCode, ProductType productType, Integer days) {
        return null;
    }

}
