package com.petrolprice.station_search_api.station.infrastructure.postgres.projection;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductPriceProjection(UUID stationId, String productType, BigDecimal price) {}
