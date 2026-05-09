package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.mapper;

import com.petrolprice.station_search_api.domain.model.Station;
import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto.StationSnapshotMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StationSnapshotMapper {
    @Mapping(target = "id", ignore = true)
    Station toModel(StationSnapshotMessage message);
}
