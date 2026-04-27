package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto;

public record FuelPriceMessage(
    String stationProductType,
    double price
) {
}
