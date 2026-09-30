package com.petrolprice.station_search_api.station.search.application.result;

public record FindStationsPage(int number, int size, int numberOfElements, boolean hasNext, boolean hasPrevious) {}
