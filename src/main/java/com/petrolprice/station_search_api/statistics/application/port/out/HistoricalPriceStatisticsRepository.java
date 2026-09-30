package com.petrolprice.station_search_api.statistics.application.port.out;

import com.petrolprice.station_search_api.statistics.application.query.HistoricalStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.result.HistoricalPricePoint;
import java.util.List;

public interface HistoricalPriceStatisticsRepository {
    List<HistoricalPricePoint> history(HistoricalStatisticsQuery query);
}
