package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.petrolprice.station_search_api.station.search.application.searcharea.AdministrativeHierarchyNormalizer;

public record AddressMessage(
        String street,
        String postalCode,
        @JsonAlias("locality") String localityName,
        String normalizedLocalityName,
        String adminArea1Name,
        @JsonAlias("province") String adminArea2Name,
        @JsonAlias("municipality") String adminArea3Name) {

    public AddressMessage {
        if (normalizedLocalityName == null && localityName != null) {
            normalizedLocalityName = AdministrativeHierarchyNormalizer.normalize(localityName);
        }
    }
}
