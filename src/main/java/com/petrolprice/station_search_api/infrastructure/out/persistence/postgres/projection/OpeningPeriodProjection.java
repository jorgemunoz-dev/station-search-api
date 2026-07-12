package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.projection;

import java.time.LocalTime;
import java.util.UUID;

public record OpeningPeriodProjection (
    UUID stationId,
    String dayOfWeek,
    LocalTime openTime,
    LocalTime closeTime
) {
}
