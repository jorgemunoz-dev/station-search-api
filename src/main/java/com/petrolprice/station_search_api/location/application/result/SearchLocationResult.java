package com.petrolprice.station_search_api.location.application.result;

import com.petrolprice.station_search_api.location.domain.SearchLocationType;

public record SearchLocationResult(
        SearchLocationType type,
        String primaryText,
        String secondaryText,
        String countryCode,
        String postalCode,
        double latitude,
        double longitude) {}
