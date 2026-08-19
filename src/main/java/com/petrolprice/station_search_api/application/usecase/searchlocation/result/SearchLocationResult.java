package com.petrolprice.station_search_api.application.usecase.searchlocation.result;

import com.petrolprice.station_search_api.application.usecase.searchlocation.SearchLocationType;

public record SearchLocationResult(
        SearchLocationType type,
        String primaryText,
        String secondaryText,
        String countryCode,
        String postalCode,
        double latitude,
        double longitude) {}
