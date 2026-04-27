package com.petrolprice.station_search_api.domain.model;

import com.petrolprice.station_search_api.domain.type.Country;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Station {
    private String externalId;
    private Country country;
    private String brand;
    private List<OpeningPeriod> openingPeriods;
    private Address address;
    private GeoLocation location;
    private List<FuelPrice> fuelPrices;
}
