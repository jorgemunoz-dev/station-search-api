package com.petrolprice.station_search_api.platform.rest.exception;

public class InvalidStationSearchRequestException extends RuntimeException {

    public InvalidStationSearchRequestException(String message) {
        super(message);
    }
}
