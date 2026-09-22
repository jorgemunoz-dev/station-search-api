package com.petrolprice.station_search_api.statistics.application;

import com.petrolprice.station_search_api.statistics.application.query.GeographicScope;
import com.petrolprice.station_search_api.statistics.application.result.FuelPriceSummaryResult;
import com.petrolprice.station_search_api.statistics.domain.ProductType;

public interface GetFuelPriceSummaryUseCase {
    FuelPriceSummaryResult getFuelPriceSummary(
            String countryCode, ProductType productType, Integer days, GeographicScope scope);
}
