package com.petrolprice.station_search_api.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String street;
    private String postalCode;
    private String locality;
    private String municipality;
    private String province;
}
