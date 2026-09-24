package com.petrolprice.station_search_api.statistics.application;

import com.petrolprice.station_search_api.statistics.application.query.CurrentStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.HistoricalStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.result.AdministrativeArea;
import com.petrolprice.station_search_api.statistics.application.result.CurrentPriceStatistics;
import com.petrolprice.station_search_api.statistics.application.result.FuelSaving;
import com.petrolprice.station_search_api.statistics.application.result.HistoricalPricePoint;
import com.petrolprice.station_search_api.statistics.application.result.RankedAreaStatistics;
import com.petrolprice.station_search_api.statistics.domain.ProductType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface FuelPriceStatisticsUseCase {
    CurrentPriceStatistics current(CurrentStatisticsQuery query);

    List<HistoricalPricePoint> history(HistoricalStatisticsQuery query);

    List<RankedAreaStatistics> areaRanking(
            String countryCode, ProductType productType, UUID parentAreaId, String areaType);

    List<AdministrativeArea> findAreas(String countryCode, UUID parentAreaId, String areaType);

    FuelSaving saving(UUID stationId, ProductType productType, BigDecimal referencePrice, BigDecimal tankLiters);
}
