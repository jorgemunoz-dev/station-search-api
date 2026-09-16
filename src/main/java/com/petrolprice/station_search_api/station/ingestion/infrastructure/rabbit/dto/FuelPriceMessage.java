package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto;

public record FuelPriceMessage(String fuelType, double price) {}
