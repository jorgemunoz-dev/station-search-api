package com.petrolprice.station_search_api.statistics.application.port.out;

import com.petrolprice.station_search_api.statistics.application.query.RadiusStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.result.RadiusPriceStatistics;
import java.util.Optional;

public interface GeospatialPriceStatisticsRepository {
    Optional<RadiusPriceStatistics> around(RadiusStatisticsQuery query);
}
