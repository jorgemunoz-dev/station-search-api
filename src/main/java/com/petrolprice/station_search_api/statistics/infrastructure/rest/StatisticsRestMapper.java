package com.petrolprice.station_search_api.statistics.infrastructure.rest;

import com.petrolprice.station_search_api.contract.rest.model.CurrentPriceStatisticsResponse;
import com.petrolprice.station_search_api.contract.rest.model.FuelPriceSummaryResponse;
import com.petrolprice.station_search_api.contract.rest.model.FuelSavingResponse;
import com.petrolprice.station_search_api.contract.rest.model.HistoricalPricePointResponse;
import com.petrolprice.station_search_api.contract.rest.model.RadiusPriceStatisticsResponse;
import com.petrolprice.station_search_api.contract.rest.model.RankedAreaStatisticsResponse;
import com.petrolprice.station_search_api.statistics.application.result.CurrentPriceStatistics;
import com.petrolprice.station_search_api.statistics.application.result.FuelPriceSummaryResult;
import com.petrolprice.station_search_api.statistics.application.result.FuelSaving;
import com.petrolprice.station_search_api.statistics.application.result.HistoricalPricePoint;
import com.petrolprice.station_search_api.statistics.application.result.RadiusPriceStatistics;
import com.petrolprice.station_search_api.statistics.application.result.RankedAreaStatistics;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StatisticsRestMapper {
    FuelPriceSummaryResponse toResponse(FuelPriceSummaryResult result);

    FuelSavingResponse toResponse(FuelSaving result);

    CurrentPriceStatisticsResponse toResponse(CurrentPriceStatistics result);

    HistoricalPricePointResponse toResponse(HistoricalPricePoint result);

    RadiusPriceStatisticsResponse toResponse(RadiusPriceStatistics result);

    RankedAreaStatisticsResponse toResponse(RankedAreaStatistics result);

    default List<HistoricalPricePointResponse> toHistoryResponse(List<HistoricalPricePoint> results) {
        return results.stream().map(this::toResponse).toList();
    }

    default List<RankedAreaStatisticsResponse> toRankingResponse(List<RankedAreaStatistics> results) {
        return results.stream().map(this::toResponse).toList();
    }
}
