package com.petrolprice.station_search_api.application.port.out;

import com.petrolprice.station_search_api.domain.model.ProductPrice;
import java.util.List;
import java.util.UUID;

public interface HistoricalPriceRepositoryPort {
    void insertSnapshot(UUID stationId, List<ProductPrice> productPrices);
}
