package com.petrolprice.station_search_api.station.domain.model;

import com.petrolprice.station_search_api.station.domain.type.ProductType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPrice {
    private ProductType productType;
    private BigDecimal price;
}
