package com.petrolprice.station_search_api.domain.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocation {
    private BigDecimal latitude;
    private BigDecimal longitude;
}
