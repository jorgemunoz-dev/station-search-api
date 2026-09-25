package com.petrolprice.station_search_api.statistics.application.port.out;

import com.petrolprice.station_search_api.statistics.application.query.CurrentStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.result.CurrentPriceStatistics;
import com.petrolprice.station_search_api.statistics.application.result.RankedLocalityStatistics;
import com.petrolprice.station_search_api.statistics.domain.ProductType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurrentPriceStatisticsRepository {
    Optional<CurrentPriceStatistics> current(CurrentStatisticsQuery query);

    List<RankedLocalityStatistics> localities(
            String countryCode,
            ProductType productType,
            String adminArea1,
            String adminArea2,
            String adminArea3);

    Optional<BigDecimal> stationPrice(UUID stationId, ProductType productType);
}
