package com.petrolprice.station_search_api.statistics.application;

import com.petrolprice.station_search_api.statistics.application.command.CalculateFuelPriceStatisticsCommand;

public interface CalculateFuelPriceStatisticsUseCase {
    void calculate(CalculateFuelPriceStatisticsCommand command);
}
