package com.petrolprice.station_search_api.station.domain.model;

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
    private String localityName;
    private String normalizedLocalityName;
    private String adminArea1Name;
    private String adminArea2Name;
    private String adminArea3Name;
}
