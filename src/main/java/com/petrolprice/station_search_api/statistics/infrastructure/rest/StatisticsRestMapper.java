package com.petrolprice.station_search_api.statistics.infrastructure.rest;

import com.petrolprice.station_search_api.contract.rest.model.*;
import com.petrolprice.station_search_api.statistics.application.result.CurrentPriceStatistics;
import com.petrolprice.station_search_api.statistics.application.result.FuelPriceSummaryResult;
import com.petrolprice.station_search_api.statistics.application.result.FuelSaving;
import com.petrolprice.station_search_api.statistics.application.result.HistoricalPricePoint;
import com.petrolprice.station_search_api.statistics.application.result.RankedLocalityStatistics;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StatisticsRestMapper {
    FuelPriceSummaryResponse toResponse(FuelPriceSummaryResult result);

    FuelSavingResponse toResponse(FuelSaving result);

    CurrentPriceStatisticsResponse toResponse(CurrentPriceStatistics result);

    HistoricalPricePointResponse toResponse(HistoricalPricePoint result);

    RankedLocalityStatisticsResponse toResponse(RankedLocalityStatistics result);

    default OffsetDateTime map(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    default List<HistoricalPricePointResponse> toHistoryResponse(List<HistoricalPricePoint> results) {
        return results.stream().map(this::toResponse).toList();
    }

    default List<RankedLocalityStatisticsResponse> toRankingResponse(List<RankedLocalityStatistics> results) {
        return results.stream().map(this::toResponse).toList();
    }
}
