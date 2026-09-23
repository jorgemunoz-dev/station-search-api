package com.petrolprice.station_search_api.statistics.application.query;

import com.petrolprice.station_search_api.statistics.domain.GeographicLevel;
import java.util.UUID;

public record GeographicScope(UUID areaId, String areaType, GeographicLevel level, String name, String province) {
    public GeographicScope {
        if (areaId == null && level == null) {
            throw new IllegalArgumentException("Geographic level is required");
        }
        if (areaId == null && level != GeographicLevel.NATIONAL && (name == null || name.isBlank())) {
            throw new IllegalArgumentException("A geographic name is required for " + level);
        }
        if (areaId == null && level == GeographicLevel.MUNICIPALITY && (province == null || province.isBlank())) {
            throw new IllegalArgumentException("province is required for a municipality scope");
        }
        name = name == null ? null : name.trim();
        province = province == null ? null : province.trim();
    }

    public static GeographicScope national() {
        return new GeographicScope(null, null, GeographicLevel.NATIONAL, null, null);
    }

    public static GeographicScope province(String province) {
        return new GeographicScope(null, "PROVINCE", GeographicLevel.PROVINCE, province, province);
    }

    public static GeographicScope municipality(String province, String municipality) {
        return new GeographicScope(null, "MUNICIPALITY", GeographicLevel.MUNICIPALITY, municipality, province);
    }

    public static GeographicScope administrativeArea(UUID areaId) {
        if (areaId == null) {
            throw new IllegalArgumentException("areaId is required");
        }
        return new GeographicScope(areaId, null, null, null, null);
    }

    public static GeographicScope resolvedAdministrativeArea(
            UUID areaId, String areaType, GeographicLevel legacyLevel, String name, String parentName) {
        return new GeographicScope(areaId, areaType, legacyLevel, name, parentName);
    }
}
