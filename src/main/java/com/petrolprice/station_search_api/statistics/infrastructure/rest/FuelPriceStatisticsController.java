package com.petrolprice.station_search_api.statistics.infrastructure.rest;

import com.petrolprice.station_search_api.contract.rest.model.CurrentPriceStatisticsResponse;
import com.petrolprice.station_search_api.contract.rest.model.FuelPriceSummaryResponse;
import com.petrolprice.station_search_api.contract.rest.model.FuelSavingResponse;
import com.petrolprice.station_search_api.contract.rest.model.GeographicLevel;
import com.petrolprice.station_search_api.contract.rest.model.HistoricalPricePointResponse;
import com.petrolprice.station_search_api.contract.rest.model.ProductType;
import com.petrolprice.station_search_api.contract.rest.model.RadiusPriceStatisticsResponse;
import com.petrolprice.station_search_api.contract.rest.model.RankedAreaStatisticsResponse;
import com.petrolprice.station_search_api.statistics.application.FuelPriceStatisticsUseCase;
import com.petrolprice.station_search_api.statistics.application.GetFuelPriceSummaryUseCase;
import com.petrolprice.station_search_api.statistics.application.StatisticsNotFoundException;
import com.petrolprice.station_search_api.statistics.application.query.CurrentStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.GeographicScope;
import com.petrolprice.station_search_api.statistics.application.query.HistoricalStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.RadiusStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.result.FuelPriceSummaryResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics/fuel-prices")
@RequiredArgsConstructor
public class FuelPriceStatisticsController {
    private final GetFuelPriceSummaryUseCase summaryUseCase;
    private final FuelPriceStatisticsUseCase statistics;
    private final StatisticsRestMapper mapper;

    @GetMapping("/summary")
    public ResponseEntity<FuelPriceSummaryResponse> getFuelPriceSummary(
            @RequestParam String countryCode,
            @RequestParam ProductType productType,
            @RequestParam(defaultValue = "7") Integer days) {
        FuelPriceSummaryResult result = summaryUseCase.getFuelPriceSummary(countryCode, product(productType), days);
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    @GetMapping("/current")
    public ResponseEntity<CurrentPriceStatisticsResponse> getCurrentFuelPriceStatistics(
            @RequestParam String countryCode,
            @RequestParam ProductType productType,
            @RequestParam(defaultValue = "NATIONAL") GeographicLevel level,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String province) {
        var result = statistics.current(
                new CurrentStatisticsQuery(countryCode, product(productType), scope(level, area, province)));
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    @GetMapping("/history")
    public ResponseEntity<List<HistoricalPricePointResponse>> getHistoricalFuelPriceStatistics(
            @RequestParam String countryCode,
            @RequestParam ProductType productType,
            @RequestParam(defaultValue = "NATIONAL") GeographicLevel level,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String province,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        var result = statistics.history(new HistoricalStatisticsQuery(
                countryCode, product(productType), scope(level, area, province), from, to));
        return ResponseEntity.ok(mapper.toHistoryResponse(result));
    }

    @GetMapping("/around")
    public ResponseEntity<RadiusPriceStatisticsResponse> getRadiusFuelPriceStatistics(
            @RequestParam String countryCode,
            @RequestParam ProductType productType,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Integer radiusMeters) {
        var result = statistics.around(new RadiusStatisticsQuery(
                countryCode,
                product(productType),
                BigDecimal.valueOf(latitude),
                BigDecimal.valueOf(longitude),
                radiusMeters));
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    @GetMapping("/provinces")
    public ResponseEntity<List<RankedAreaStatisticsResponse>> getProvinceFuelPriceStatistics(
            @RequestParam String countryCode, @RequestParam ProductType productType) {
        return ResponseEntity.ok(mapper.toRankingResponse(statistics.provinceRanking(countryCode, product(productType))));
    }

    @GetMapping("/savings")
    public ResponseEntity<FuelSavingResponse> getFuelSaving(
            @RequestParam UUID stationId,
            @RequestParam ProductType productType,
            @RequestParam BigDecimal referencePrice,
            @RequestParam(required = false) BigDecimal tankLiters) {
        return ResponseEntity.ok(mapper.toResponse(
                statistics.saving(stationId, product(productType), referencePrice, tankLiters)));
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
        };
    }
}
