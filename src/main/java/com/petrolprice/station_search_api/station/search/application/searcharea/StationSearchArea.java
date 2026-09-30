package com.petrolprice.station_search_api.station.search.application.searcharea;

public sealed interface StationSearchArea permits LocalitySearchArea, RadiusSearchArea, ViewportSearchArea {}
