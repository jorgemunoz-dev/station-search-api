package com.petrolprice.station_search_api.statistics.application;

import com.petrolprice.station_search_api.statistics.application.result.FuelPriceSummaryResult;
import com.petrolprice.station_search_api.statistics.domain.ProductType;
import org.springframework.stereotype.Service;

@Service
public class GetFuelPriceSummaryService implements GetFuelPriceSummaryUseCase {

    @Override
    public FuelPriceSummaryResult getFuelPriceSummary(String countryCode, ProductType productType, Integer days) {
        return null;
    }

}
