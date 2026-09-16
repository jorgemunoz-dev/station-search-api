package com.petrolprice.station_search_api.application.usecase.statistics.result;

public record EstimatedSaving(
    Double pricePerLiter,
    Double tankSizeLiters,
    Double amount
) {
}
