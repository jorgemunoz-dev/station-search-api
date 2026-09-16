package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto;

import java.util.List;

public record StationSnapshotPayload(
    String externalId,
    String country,
    String brand,
    String normalizedBrand,
    List<OpeningPeriodMessage> schedule,
    AddressMessage address,
    LocationMessage location,
    List<FuelPriceMessage> fuelPrices
) {}
