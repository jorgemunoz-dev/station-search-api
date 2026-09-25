package com.petrolprice.station_search_api.station.search.infrastructure.rest.factory;

import com.petrolprice.station_search_api.contract.rest.model.StationSearchSortBy;
import com.petrolprice.station_search_api.platform.rest.exception.InvalidStationSearchRequestException;
import com.petrolprice.station_search_api.station.search.application.query.FindStationsPageRequest;
import com.petrolprice.station_search_api.station.search.application.query.FindStationsQuery;
import com.petrolprice.station_search_api.station.search.application.query.FindStationsSort;
import com.petrolprice.station_search_api.station.search.application.searcharea.LocalitySearchArea;
import com.petrolprice.station_search_api.station.search.application.searcharea.RadiusSearchArea;
import com.petrolprice.station_search_api.station.search.application.searcharea.StationSearchArea;
import com.petrolprice.station_search_api.station.search.application.searcharea.ViewportSearchArea;
import com.petrolprice.station_search_api.station.search.infrastructure.rest.request.StationSearchParameters;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class FindStationsQueryFactory {

    public FindStationsQuery create(StationSearchParameters parameters) {
        Objects.requireNonNull(parameters, "parameters is required");

        if (parameters.searchMode() == null) {
            throw new InvalidStationSearchRequestException("searchMode is required");
        }

        StationSearchArea searchArea =
                switch (parameters.searchMode()) {
                    case RADIUS -> createRadiusSearchArea(parameters);
                    case VIEWPORT -> createViewportSearchArea(parameters);
                    case LOCALITY -> createLocalitySearchArea(parameters);
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

    private RadiusSearchArea createRadiusSearchArea(StationSearchParameters parameters) {
        requireRadiusParameters(parameters);
        rejectViewportParameters(parameters);
        rejectLocalityParameters(parameters);

        return new RadiusSearchArea(parameters.latitude(), parameters.longitude(), parameters.radiusMeters());
    }

    private ViewportSearchArea createViewportSearchArea(StationSearchParameters parameters) {
        requireViewportParameters(parameters);
        rejectRadiusParameters(parameters);
        rejectLocalityParameters(parameters);

        return new ViewportSearchArea(parameters.north(), parameters.south(), parameters.east(), parameters.west());
    }

    private LocalitySearchArea createLocalitySearchArea(StationSearchParameters parameters) {
        rejectGeographicalParameters(parameters);
        if (parameters.countryCode() == null || parameters.locality() == null) {
            throw new InvalidStationSearchRequestException("LOCALITY search requires countryCode and locality");
        }
        if (!parameters.countryCode().matches("[A-Za-z]{2}")) {
            throw new InvalidStationSearchRequestException("countryCode must be a two-letter ISO country code");
        }
        if (parameters.locality().isBlank()) {
            throw new InvalidStationSearchRequestException("locality must not be blank");
        }
        return new LocalitySearchArea(parameters.countryCode(), parameters.locality());
    }

    private void rejectGeographicalParameters(StationSearchParameters parameters) {
        if (parameters.latitude() != null
                || parameters.longitude() != null
                || parameters.radiusMeters() != null
                || parameters.north() != null
                || parameters.south() != null
                || parameters.east() != null
                || parameters.west() != null) {
            throw new InvalidStationSearchRequestException(
                    "LOCALITY search must not contain radius or viewport parameters");
        }
    }

    private void rejectLocalityParameters(StationSearchParameters parameters) {
        if (parameters.countryCode() != null || parameters.locality() != null) {
            throw new InvalidStationSearchRequestException(
                    "RADIUS and VIEWPORT searches must not contain countryCode or locality");
        }
    }

    private void requireRadiusParameters(StationSearchParameters parameters) {
        if (parameters.latitude() == null || parameters.longitude() == null || parameters.radiusMeters() == null) {
            throw new InvalidStationSearchRequestException("RADIUS search requires lat, lng and radiusMeters");
        }
    }

    private void requireViewportParameters(StationSearchParameters parameters) {
        if (parameters.north() == null
                || parameters.south() == null
                || parameters.east() == null
                || parameters.west() == null) {
            throw new InvalidStationSearchRequestException("VIEWPORT search requires north, south, east and west");
        }
    }

    private void rejectRadiusParameters(StationSearchParameters parameters) {
        if (parameters.latitude() != null || parameters.longitude() != null || parameters.radiusMeters() != null) {
            throw new InvalidStationSearchRequestException("VIEWPORT search must not contain lat, lng or radiusMeters");
        }
    }

    private void rejectViewportParameters(StationSearchParameters parameters) {
        if (parameters.north() != null
                || parameters.south() != null
                || parameters.east() != null
                || parameters.west() != null) {
            throw new InvalidStationSearchRequestException("RADIUS search must not contain north, south, east or west");
        }
    }

    private com.petrolprice.station_search_api.station.domain.type.ProductType mapProductType(
            com.petrolprice.station_search_api.contract.rest.model.ProductType productType) {
        if (productType == null) {
            return null;
        }

        return com.petrolprice.station_search_api.station.domain.type.ProductType.valueOf(productType.name());
    }

    private FindStationsSort mapSort(StationSearchSortBy sortBy) {
        if (sortBy == null) {
            return null;
        }

        return FindStationsSort.valueOf(sortBy.name());
    }
}
