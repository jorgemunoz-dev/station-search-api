package com.petrolprice.station_search_api.statistics.application.result;

import java.math.BigDecimal;

public record FuelSaving(
        BigDecimal stationPrice,
        BigDecimal referencePrice,
        BigDecimal savingPerLiter,
        BigDecimal tankLiters,
        BigDecimal savingPerTank) {}
