package com.petrolprice.station_search_api.statistics.application;

import com.petrolprice.station_search_api.statistics.application.port.out.CurrentPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.port.out.GeospatialPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.port.out.HistoricalPriceStatisticsRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FuelPriceStatisticsService implements FuelPriceStatisticsUseCase {
    private final CurrentPriceStatisticsRepository currentRepository;
    private final HistoricalPriceStatisticsRepository historicalRepository;
    private final GeospatialPriceStatisticsRepository geospatialRepository;
    private final FuelSavingCalculator savingCalculator;

    @Override
    public CurrentPriceStatistics current(CurrentStatisticsQuery query) {
        return currentRepository
                .current(query)
                .orElseThrow(() -> new StatisticsNotFoundException("No prices match the query"));
    }

    @Override
    public List<HistoricalPricePoint> history(HistoricalStatisticsQuery query) {
        return historicalRepository.history(query);
    }

    @Override
    public RadiusPriceStatistics around(RadiusStatisticsQuery query) {
        return geospatialRepository
                .around(query)
                .orElseThrow(() -> new StatisticsNotFoundException("No stations match the radius"));
    }

    @Override
    public List<RankedAreaStatistics> provinceRanking(String countryCode, ProductType productType) {
        return currentRepository.provinces(countryCode, productType);
    }

    @Override
    public FuelSaving saving(
            UUID stationId, ProductType productType, BigDecimal referencePrice, BigDecimal tankLiters) {
        BigDecimal stationPrice = currentRepository
                .stationPrice(stationId, productType)
                .orElseThrow(() -> new StatisticsNotFoundException("The station has no current price for the product"));
        return savingCalculator.calculate(stationPrice, referencePrice, tankLiters);
    }
}
