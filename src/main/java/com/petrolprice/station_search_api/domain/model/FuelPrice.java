package com.petrolprice.station_search_api.domain.model;

import com.petrolprice.station_search_api.domain.type.StationProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuelPrice {
    private StationProductType stationProductType;
    private BigDecimal price;
}
