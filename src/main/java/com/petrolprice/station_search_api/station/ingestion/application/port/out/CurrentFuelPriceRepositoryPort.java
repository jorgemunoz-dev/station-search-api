package com.petrolprice.station_search_api.station.ingestion.application.port.out;

import com.petrolprice.station_search_api.station.domain.model.ProductPrice;
import java.util.List;
import java.util.UUID;

public interface CurrentFuelPriceRepositoryPort {
    void replaceCurrentPrices(UUID stationId, List<ProductPrice> productPrices);
}
