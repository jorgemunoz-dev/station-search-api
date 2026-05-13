package com.petrolprice.station_search_api.application.port.out;

import com.petrolprice.station_search_api.domain.model.FuelPrice;
import java.util.List;
import java.util.UUID;

public interface CurrentFuelPriceRepositoryPort {
    void replaceCurrentPrices(UUID stationId, List<FuelPrice> fuelPrices);
}
