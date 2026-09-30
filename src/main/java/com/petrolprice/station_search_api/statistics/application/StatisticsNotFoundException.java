package com.petrolprice.station_search_api.statistics.application;

public class StatisticsNotFoundException extends RuntimeException {
    public StatisticsNotFoundException(String message) {
        super(message);
    }
}
