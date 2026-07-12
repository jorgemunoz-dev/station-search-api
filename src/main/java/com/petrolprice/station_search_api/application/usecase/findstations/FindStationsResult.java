package com.petrolprice.station_search_api.application.usecase.findstations;

import java.util.List;

public record FindStationsResult (
    List<FindStationsItem> stations
) {
}
