package com.petrolprice.station_search_api.statistics.application.query;

import com.petrolprice.station_search_api.statistics.domain.GeographicLevel;

public record GeographicScope(GeographicLevel level, String name, String province) {
    public GeographicScope {
        if (level == null) {
            throw new IllegalArgumentException("Geographic level is required");
        }
        if (level != GeographicLevel.NATIONAL && (name == null || name.isBlank())) {
            throw new IllegalArgumentException("A geographic name is required for " + level);
        }
        if (level == GeographicLevel.MUNICIPALITY && (province == null || province.isBlank())) {
            throw new IllegalArgumentException("province is required for a municipality scope");
        }
        name = name == null ? null : name.trim();
        province = province == null ? null : province.trim();
    }

    public static GeographicScope national() {
        return new GeographicScope(GeographicLevel.NATIONAL, null, null);
    }

    public static GeographicScope province(String province) {
        return new GeographicScope(GeographicLevel.PROVINCE, province, province);
    }

    public static GeographicScope municipality(String province, String municipality) {
        return new GeographicScope(GeographicLevel.MUNICIPALITY, municipality, province);
    }
}
