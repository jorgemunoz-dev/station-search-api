package com.petrolprice.station_search_api.statistics.application.query;

public record GeographicScope(String adminArea1Name, String adminArea2Name, String adminArea3Name) {

    public static GeographicScope country() {
        return new GeographicScope(null, null, null);
    }

    public static GeographicScope administrativeHierarchy(String adminArea1, String adminArea2, String adminArea3) {
        String area1 = blankToNull(adminArea1);
        String area2 = blankToNull(adminArea2);
        String area3 = blankToNull(adminArea3);
        if (area1 == null && area2 == null && area3 == null) {
            return country();
        }
        if (area1 == null || (area3 != null && area2 == null)) {
            throw new IllegalArgumentException(
                    "Administrative areas must be supplied in order: adminArea1, adminArea2, adminArea3");
        }
        return new GeographicScope(area1, area2, area3);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
