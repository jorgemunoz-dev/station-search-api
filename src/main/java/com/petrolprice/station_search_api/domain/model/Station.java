package com.petrolprice.station_search_api.domain.model;

import com.petrolprice.station_search_api.domain.type.Country;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Station {
    private UUID id;
    private String externalId;
    private Country country;
    private String brand;
    private List<OpeningPeriod> openingPeriods;
    private Address address;
    private GeoLocation location;
    private List<ProductPrice> productPrices;
}
