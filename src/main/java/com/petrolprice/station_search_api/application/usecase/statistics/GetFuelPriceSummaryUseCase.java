package com.petrolprice.station_search_api.application.usecase.statistics;

import com.petrolprice.station_search_api.application.usecase.statistics.result.FuelPriceSummaryResult;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.ProductType;

public interface GetFuelPriceSummaryUseCase {
    FuelPriceSummaryResult getFuelPriceSummary(String countryCode, ProductType productType, Integer days);
}
