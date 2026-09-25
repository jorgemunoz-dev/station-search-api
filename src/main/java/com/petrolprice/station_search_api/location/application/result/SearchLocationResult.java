package com.petrolprice.station_search_api.location.application.result;

import com.petrolprice.station_search_api.location.domain.SearchLocationType;

public record SearchLocationResult(
        SearchLocationType type,
        String primaryText,
        String secondaryText,
        String countryCode,
        String postalCode,
        String normalizedLocalityName,
        String adminArea1Name,
        String adminArea1Code,
        String adminArea2Name,
        String adminArea2Code,
        String adminArea3Name,
        String adminArea3Code,
        double latitude,
        double longitude) {}
