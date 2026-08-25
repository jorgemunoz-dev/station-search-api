package com.petrolprice.station_search_api.application.usecase.findstations.result;

import com.petrolprice.station_search_api.domain.model.Station;
import java.math.BigDecimal;

public record FindStationsItem(Station station, BigDecimal distanceMeters) {}
