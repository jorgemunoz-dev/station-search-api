package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.mapper;

import com.petrolprice.station_search_api.domain.model.Station;
import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto.StationSnapshotMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StationSnapshotMapper {
    Station toModel(StationSnapshotMessage message);
}
