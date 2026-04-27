package com.petrolprice.station_search_api.domain.model;

import com.petrolprice.station_search_api.domain.type.Day;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

@Data
public class OpeningPeriod {
    List<Day> days;
    LocalTime open;
    LocalTime close;
}
