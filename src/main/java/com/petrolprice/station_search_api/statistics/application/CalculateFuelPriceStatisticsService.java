package com.petrolprice.station_search_api.statistics.application;

import com.petrolprice.station_search_api.statistics.application.command.CalculateFuelPriceStatisticsCommand;
import com.petrolprice.station_search_api.statistics.application.port.out.StatisticsCalculationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalculateFuelPriceStatisticsService implements CalculateFuelPriceStatisticsUseCase {
    private final StatisticsCalculationRepository repository;

    @Override
    public void calculate(CalculateFuelPriceStatisticsCommand command) {
        repository.replaceForSnapshot(command.snapshotId());
    }
}
