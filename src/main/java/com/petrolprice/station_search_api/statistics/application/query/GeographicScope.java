package com.petrolprice.station_search_api.statistics.application.query;

import java.util.UUID;

public record GeographicScope(
        UUID areaId, String areaType, String name, UUID parentAreaId, String parentAreaName) {

    public static GeographicScope country() {
        return new GeographicScope(null, null, null, null, null);
    }

    public static GeographicScope administrativeArea(UUID areaId) {
        if (areaId == null) {
            throw new IllegalArgumentException("areaId is required");
        }
        return new GeographicScope(areaId, null, null, null, null);
    }

    public static GeographicScope resolvedAdministrativeArea(
            UUID areaId, String areaType, String name, UUID parentAreaId, String parentAreaName) {
        return new GeographicScope(areaId, areaType, name, parentAreaId, parentAreaName);
    }
}
