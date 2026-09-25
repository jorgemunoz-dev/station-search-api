package com.petrolprice.station_search_api.station.search.infrastructure.rest.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petrolprice.station_search_api.contract.rest.model.StationSearchMode;
import com.petrolprice.station_search_api.contract.rest.model.StationSearchSortBy;
import com.petrolprice.station_search_api.platform.rest.exception.InvalidStationSearchRequestException;
import com.petrolprice.station_search_api.station.search.application.query.FindStationsSort;
import com.petrolprice.station_search_api.station.search.application.searcharea.LocalitySearchArea;
import com.petrolprice.station_search_api.station.search.infrastructure.rest.request.StationSearchParameters;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FindStationsQueryFactoryTest {
    private final FindStationsQueryFactory factory = new FindStationsQueryFactory();

    @Test
    void shouldCreateExactLocalitySearch() {
        var query = factory.create(StationSearchParameters.builder()
                .searchMode(StationSearchMode.LOCALITY)
                .countryCode("es")
                .locality("Málaga")
                .sortBy(StationSearchSortBy.PRICE)
                .size(50)
                .build());

        assertThat(query.searchArea()).isEqualTo(new LocalitySearchArea("ES", "malaga"));
        assertThat(query.sortBy()).isEqualTo(FindStationsSort.PRICE);
    }

    @Test
    void shouldRejectCoordinatesInLocalitySearch() {
        StationSearchParameters parameters = StationSearchParameters.builder()
                .searchMode(StationSearchMode.LOCALITY)
                .countryCode("ES")
                .locality("Málaga")
                .latitude(BigDecimal.ONE)
                .sortBy(StationSearchSortBy.PRICE)
                .size(50)
                .build();

        assertThatThrownBy(() -> factory.create(parameters))
                .isInstanceOf(InvalidStationSearchRequestException.class)
                .hasMessage("LOCALITY search must not contain radius or viewport parameters");
    }

    @Test
    void shouldRejectLocalityParametersInRadiusSearch() {
        StationSearchParameters parameters = StationSearchParameters.builder()
                .searchMode(StationSearchMode.RADIUS)
                .countryCode("ES")
                .locality("Málaga")
                .latitude(BigDecimal.ONE)
                .longitude(BigDecimal.ONE)
                .radiusMeters(1_000)
                .sortBy(StationSearchSortBy.PRICE)
                .size(50)
                .build();

        assertThatThrownBy(() -> factory.create(parameters))
                .isInstanceOf(InvalidStationSearchRequestException.class)
                .hasMessage("RADIUS and VIEWPORT searches must not contain countryCode or locality");
    }
}
