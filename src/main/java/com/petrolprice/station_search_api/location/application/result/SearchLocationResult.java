package com.petrolprice.station_search_api.location.application.result;

import com.petrolprice.station_search_api.location.domain.SearchLocationType;

public record SearchLocationResult(
        SearchLocationType type,
        String primaryText,
        String secondaryText,
        String countryCode,
        String postalCode,
        long localityId,
        String stationLocalityName,
        String normalizedLocalityName,
        Long adminArea1Id,
        String adminArea1Name,
        String adminArea1Code,
        Long adminArea2Id,
        String adminArea2Name,
        String adminArea2Code,
        Long adminArea3Id,
        String adminArea3Name,
        String adminArea3Code,
        double latitude,
        double longitude) {}
