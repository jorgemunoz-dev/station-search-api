package com.petrolprice.station_search_api.statistics.application;

import com.petrolprice.station_search_api.statistics.application.query.CurrentStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.HistoricalStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.RadiusStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.result.CurrentPriceStatistics;
import com.petrolprice.station_search_api.statistics.application.result.HistoricalPricePoint;
import com.petrolprice.station_search_api.statistics.application.result.FuelSaving;
import com.petrolprice.station_search_api.statistics.application.result.RadiusPriceStatistics;
import com.petrolprice.station_search_api.statistics.application.result.RankedAreaStatistics;
import com.petrolprice.station_search_api.statistics.domain.ProductType;
import java.util.List;
import java.math.BigDecimal;
import java.util.UUID;

public interface FuelPriceStatisticsUseCase {
    CurrentPriceStatistics current(CurrentStatisticsQuery query);

    List<HistoricalPricePoint> history(HistoricalStatisticsQuery query);

    RadiusPriceStatistics around(RadiusStatisticsQuery query);

    List<RankedAreaStatistics> provinceRanking(String countryCode, ProductType productType);

    FuelSaving saving(UUID stationId, ProductType productType, BigDecimal referencePrice, BigDecimal tankLiters);
}
