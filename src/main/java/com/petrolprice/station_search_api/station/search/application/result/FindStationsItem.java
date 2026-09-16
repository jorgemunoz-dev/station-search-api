package com.petrolprice.station_search_api.station.search.application.result;

import com.petrolprice.station_search_api.station.domain.model.Station;
import java.math.BigDecimal;

public record FindStationsItem(Station station, BigDecimal distanceMeters) {}
