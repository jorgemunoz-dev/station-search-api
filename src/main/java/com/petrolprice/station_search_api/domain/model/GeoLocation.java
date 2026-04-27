package com.petrolprice.station_search_api.domain.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocation {
    private BigDecimal latitude;
    private BigDecimal longitude;
}
