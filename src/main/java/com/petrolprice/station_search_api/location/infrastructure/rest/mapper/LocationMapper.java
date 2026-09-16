package com.petrolprice.station_search_api.location.infrastructure.rest.mapper;

import com.petrolprice.station_search_api.location.application.result.SearchLocationResult;
import com.petrolprice.station_search_api.contract.rest.model.LocationSuggestion;
import org.mapstruct.Mapper;
import org.openapitools.jackson.nullable.JsonNullable;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    LocationSuggestion toDto(SearchLocationResult result);

    default <T> JsonNullable<T> map(T value) {
        return value == null ? JsonNullable.undefined() : JsonNullable.of(value);
    }
}
