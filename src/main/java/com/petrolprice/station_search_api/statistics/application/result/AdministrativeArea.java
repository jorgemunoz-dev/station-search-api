package com.petrolprice.station_search_api.statistics.application.result;

import java.util.UUID;

public record AdministrativeArea(
        UUID id, String countryCode, String type, String name, UUID parentId, String parentName) {}
