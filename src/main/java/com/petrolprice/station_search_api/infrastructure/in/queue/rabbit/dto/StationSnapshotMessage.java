package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto;

import java.util.List;

public record StationSnapshotMessage(
        String externalId,
        String country,
        String brand,
        String normalizedBrand,
        List<OpeningPeriodMessage> openingPeriods,
        AddressMessage address,
        LocationMessage location,
        List<FuelPriceMessage> fuelPrices) {}
