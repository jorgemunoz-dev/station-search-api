package com.petrolprice.station_search_api.application.usecase.findstations.searcharea;

public sealed interface StationSearchArea permits RadiusSearchArea, ViewportSearchArea {}
