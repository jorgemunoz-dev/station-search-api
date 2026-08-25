package com.petrolprice.station_search_api.application.usecase.findstations.searcharea;

public record ViewportSearchArea(double north, double south, double east, double west) implements StationSearchArea {

    public ViewportSearchArea {
        validateLatitude(north, "north");
        validateLatitude(south, "south");
        validateLongitude(east, "east");
        validateLongitude(west, "west");

        if (south >= north) {
            throw new IllegalArgumentException("south must be lower than north");
        }
    }

    private static void validateLatitude(double latitude, String fieldName) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException(fieldName + " must be between -90 and 90");
        }
    }

    private static void validateLongitude(double longitude, String fieldName) {
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(fieldName + " must be between -180 and 180");
        }
    }
}
