package com.petrolprice.station_search_api.infrastructure.in.rest.factory;

import com.petrolprice.station_search_api.application.usecase.findstations.query.FindStationsPageRequest;
import com.petrolprice.station_search_api.application.usecase.findstations.query.FindStationsQuery;
import com.petrolprice.station_search_api.application.usecase.findstations.query.FindStationsSort;
import com.petrolprice.station_search_api.application.usecase.findstations.searcharea.RadiusSearchArea;
import com.petrolprice.station_search_api.application.usecase.findstations.searcharea.StationSearchArea;
import com.petrolprice.station_search_api.application.usecase.findstations.searcharea.ViewportSearchArea;
import com.petrolprice.station_search_api.infrastructure.in.rest.dto.StationSearchSortBy;
import com.petrolprice.station_search_api.infrastructure.in.rest.exception.InvalidStationSearchRequestException;
import com.petrolprice.station_search_api.infrastructure.in.rest.request.StationSearchParameters;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class FindStationsQueryFactory {

    public FindStationsQuery create(StationSearchParameters parameters) {
        Objects.requireNonNull(parameters, "parameters is required");

        if (parameters.searchMode() == null) {
            throw new InvalidStationSearchRequestException(
                "searchMode is required"
            );
        }

        StationSearchArea searchArea = switch (parameters.searchMode()) {
            case RADIUS -> createRadiusSearchArea(parameters);
            case VIEWPORT -> createViewportSearchArea(parameters);
        };

        return FindStationsQuery.builder()
            .searchArea(searchArea)
            .productType(mapProductType(parameters.productType()))
            .sortBy(mapSort(parameters.sortBy()))
            .pageRequest(mapFindStationsPageRequest(parameters))
            .build();
    }

    private FindStationsPageRequest mapFindStationsPageRequest(StationSearchParameters parameters) {
        return FindStationsPageRequest.builder()
            .page(parameters.page())
            .size(parameters.size())
            .build();
    }

    private RadiusSearchArea createRadiusSearchArea(
        StationSearchParameters parameters
    ) {
        requireRadiusParameters(parameters);
        rejectViewportParameters(parameters);

        return new RadiusSearchArea(
            parameters.latitude(),
            parameters.longitude(),
            parameters.radiusMeters()
        );
    }

    private ViewportSearchArea createViewportSearchArea(
        StationSearchParameters parameters
    ) {
        requireViewportParameters(parameters);
        rejectRadiusParameters(parameters);

        return new ViewportSearchArea(
            parameters.north(),
            parameters.south(),
            parameters.east(),
            parameters.west()
        );
    }

    private void requireRadiusParameters(
        StationSearchParameters parameters
    ) {
        if (parameters.latitude() == null
            || parameters.longitude() == null
            || parameters.radiusMeters() == null) {
            throw new InvalidStationSearchRequestException(
                "RADIUS search requires lat, lng and radiusMeters"
            );
        }
    }

    private void requireViewportParameters(
        StationSearchParameters parameters
    ) {
        if (parameters.north() == null
            || parameters.south() == null
            || parameters.east() == null
            || parameters.west() == null) {
            throw new InvalidStationSearchRequestException(
                "VIEWPORT search requires north, south, east and west"
            );
        }
    }

    private void rejectRadiusParameters(
        StationSearchParameters parameters
    ) {
        if (parameters.latitude() != null
            || parameters.longitude() != null
            || parameters.radiusMeters() != null) {
            throw new InvalidStationSearchRequestException(
                "VIEWPORT search must not contain lat, lng or radiusMeters"
            );
        }
    }

    private void rejectViewportParameters(
        StationSearchParameters parameters
    ) {
        if (parameters.north() != null
            || parameters.south() != null
            || parameters.east() != null
            || parameters.west() != null) {
            throw new InvalidStationSearchRequestException(
                "RADIUS search must not contain north, south, east or west"
            );
        }
    }

    private com.petrolprice.station_search_api.domain.type.ProductType
    mapProductType(
        com.petrolprice.station_search_api.infrastructure.in.rest.dto.ProductType productType
    ) {
        if (productType == null) {
            return null;
        }

        return com.petrolprice.station_search_api.domain.type.ProductType
            .valueOf(productType.name());
    }

    private FindStationsSort mapSort(
        StationSearchSortBy sortBy
    ) {
        if (sortBy == null) {
            return null;
        }

        return FindStationsSort.valueOf(sortBy.name());
    }
}