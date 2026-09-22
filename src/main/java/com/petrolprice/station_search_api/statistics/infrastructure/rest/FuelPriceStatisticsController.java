package com.petrolprice.station_search_api.statistics.infrastructure.rest;

import com.petrolprice.station_search_api.contract.rest.api.StatisticsApi;
import com.petrolprice.station_search_api.contract.rest.model.*;
import com.petrolprice.station_search_api.statistics.application.FuelPriceStatisticsUseCase;
import com.petrolprice.station_search_api.statistics.application.GetFuelPriceSummaryUseCase;
import com.petrolprice.station_search_api.statistics.application.StatisticsNotFoundException;
import com.petrolprice.station_search_api.statistics.application.query.CurrentStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.GeographicScope;
import com.petrolprice.station_search_api.statistics.application.query.HistoricalStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.result.CurrentPriceStatistics;
import com.petrolprice.station_search_api.statistics.application.result.FuelPriceSummaryResult;
import com.petrolprice.station_search_api.statistics.application.result.HistoricalPricePoint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FuelPriceStatisticsController implements StatisticsApi {
    private final GetFuelPriceSummaryUseCase summaryUseCase;
    private final FuelPriceStatisticsUseCase statistics;
    private final StatisticsRestMapper mapper;

    @Override
    public ResponseEntity<CurrentPriceStatisticsResponse> getCurrentFuelPriceStatistics(
            String countryCode, ProductType productType, GeographicLevel level, String area, String province) {
        CurrentPriceStatistics result = statistics.current(
                new CurrentStatisticsQuery(countryCode, product(productType), scope(level, area, province)));
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    @Override
    public ResponseEntity<FuelPriceSummaryResponse> getFuelPriceSummary(
            String countryCode,
            ProductType productType,
            Integer days,
            GeographicLevel level,
            String area,
            String province) {
        FuelPriceSummaryResult result = summaryUseCase.getFuelPriceSummary(
                countryCode, product(productType), days, scope(level, area, province));
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    @Override
    public ResponseEntity<FuelSavingResponse> getFuelSaving(
            UUID stationId, ProductType productType, BigDecimal referencePrice, BigDecimal tankLiters) {
        return ResponseEntity.ok(
                mapper.toResponse(statistics.saving(stationId, product(productType), referencePrice, tankLiters)));
    }

    @Override
    public ResponseEntity<List<HistoricalPricePointResponse>> getHistoricalFuelPriceStatistics(
            String countryCode,
            ProductType productType,
            LocalDate from,
            LocalDate to,
            GeographicLevel level,
            String area,
            String province) {
        List<HistoricalPricePoint> result = statistics.history(new HistoricalStatisticsQuery(
                countryCode, product(productType), scope(level, area, province), from, to));
        return ResponseEntity.ok(mapper.toHistoryResponse(result));
    }

    @Override
    public ResponseEntity<List<RankedAreaStatisticsResponse>> getProvinceFuelPriceStatistics(
            String countryCode, ProductType productType) {
        return ResponseEntity.ok(
                mapper.toRankingResponse(statistics.provinceRanking(countryCode, product(productType))));
    }

    @ExceptionHandler(StatisticsNotFoundException.class)
    public ProblemDetail handleNotFound(StatisticsNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Statistics not found");
        problem.setProperty("code", "STATISTICS_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidQuery(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid statistics query");
        problem.setProperty("code", "INVALID_STATISTICS_QUERY");
        return problem;
    }

    private com.petrolprice.station_search_api.statistics.domain.ProductType product(ProductType productType) {
        return com.petrolprice.station_search_api.statistics.domain.ProductType.valueOf(productType.name());
    }

    private GeographicScope scope(GeographicLevel level, String area, String province) {
        GeographicLevel effectiveLevel = level == null ? GeographicLevel.NATIONAL : level;
        return switch (effectiveLevel) {
            case NATIONAL -> GeographicScope.national();
            case PROVINCE -> GeographicScope.province(area);
            case MUNICIPALITY -> GeographicScope.municipality(province, area);
            case LOCALITY -> GeographicScope.locality(province, area);
        };
    }
}
