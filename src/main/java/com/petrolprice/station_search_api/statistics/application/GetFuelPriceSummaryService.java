package com.petrolprice.station_search_api.statistics.application;

import com.petrolprice.station_search_api.statistics.application.query.CurrentStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.GeographicScope;
import com.petrolprice.station_search_api.statistics.application.query.HistoricalStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.result.CurrentPriceStatistics;
import com.petrolprice.station_search_api.statistics.application.result.EstimatedSaving;
import com.petrolprice.station_search_api.statistics.application.result.FuelPriceSummaryResult;
import com.petrolprice.station_search_api.statistics.application.result.HistoricalPricePoint;
import com.petrolprice.station_search_api.statistics.domain.ProductType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetFuelPriceSummaryService implements GetFuelPriceSummaryUseCase {
    private static final double DEFAULT_TANK_LITERS = 55;

    private final FuelPriceStatisticsUseCase statistics;

    @Override
    public FuelPriceSummaryResult getFuelPriceSummary(
            String countryCode, ProductType productType, Integer days, GeographicScope scope) {
        int periodDays = days == null ? 7 : days;
        if (periodDays < 1 || periodDays > 365) {
            throw new IllegalArgumentException("days must be between 1 and 365");
        }
        CurrentPriceStatistics current = statistics.current(new CurrentStatisticsQuery(countryCode, productType, scope));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<HistoricalPricePoint> history = statistics.history(new HistoricalStatisticsQuery(
            countryCode, productType, scope, today.minusDays(periodDays), today));
        BigDecimal previous = history.isEmpty() ? null : history.getFirst().averagePrice();
        BigDecimal variation = previous == null ? null : current.averagePrice().subtract(previous);
        Double percentage = previous == null || previous.signum() == 0
            ? null
            : variation.multiply(BigDecimal.valueOf(100)).divide(previous, 4, java.math.RoundingMode.HALF_UP).doubleValue();
        BigDecimal savingPerLiter = current.maximumPrice().subtract(current.minimumPrice());

        return new FuelPriceSummaryResult(
            countryCode.toUpperCase(), productType, current.averagePrice().doubleValue(),
            value(previous), value(variation), percentage, current.minimumPrice().doubleValue(),
            current.maximumPrice().doubleValue(), current.stationCount(),
            new EstimatedSaving(savingPerLiter.doubleValue(), DEFAULT_TANK_LITERS,
                savingPerLiter.multiply(BigDecimal.valueOf(DEFAULT_TANK_LITERS)).doubleValue()),
            current.updatedAt());
    }

    private Double value(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
