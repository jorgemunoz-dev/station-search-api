package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto;

public record AddressMessage(
        String street, String postalCode, String localityName, String normalizedLocalityName,
        String adminArea1Name, String adminArea2Name, String adminArea3Name) {}
