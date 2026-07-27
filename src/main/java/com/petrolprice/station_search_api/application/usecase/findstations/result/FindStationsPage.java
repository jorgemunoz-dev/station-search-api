package com.petrolprice.station_search_api.application.usecase.findstations.result;

public record FindStationsPage(int number, int size, int numberOfElements, boolean hasNext, boolean hasPrevious) {}
