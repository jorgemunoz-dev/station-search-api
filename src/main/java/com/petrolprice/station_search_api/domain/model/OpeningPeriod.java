package com.petrolprice.station_search_api.domain.model;

import com.petrolprice.station_search_api.domain.type.Day;
import java.time.LocalTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OpeningPeriod {
    List<Day> days;
    LocalTime open;
    LocalTime close;
}
