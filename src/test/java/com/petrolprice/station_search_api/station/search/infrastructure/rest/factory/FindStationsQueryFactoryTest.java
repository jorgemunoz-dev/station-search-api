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
                .adminArea1("Area 1")
                .adminArea2("Area 2")
                .adminArea3("Locality")
                .sortBy(StationSearchSortBy.PRICE)
                .size(50)
                .build());

        assertThat(query.searchArea()).isEqualTo(new LocalitySearchArea("ES", "Area 1", "Area 2", "Locality"));
        assertThat(query.sortBy()).isEqualTo(FindStationsSort.PRICE);
    }

    @Test
    void shouldRejectCoordinatesInLocalitySearch() {
        StationSearchParameters parameters = StationSearchParameters.builder()
                .searchMode(StationSearchMode.LOCALITY)
                .countryCode("ES")
                .adminArea1("Area 1")
                .adminArea2("Area 2")
                .adminArea3("Locality")
                .latitude(BigDecimal.ONE)
                .sortBy(StationSearchSortBy.PRICE)
                .size(50)
                .build();

        assertThatThrownBy(() -> factory.create(parameters))
                .isInstanceOf(InvalidStationSearchRequestException.class)
                .hasMessage("LOCALITY search must not contain radius or viewport parameters");
    }

    @Test
    void shouldRequireTheCompleteAdministrativeHierarchy() {
        StationSearchParameters parameters = StationSearchParameters.builder()
                .searchMode(StationSearchMode.LOCALITY)
                .countryCode("ES")
                .adminArea1("Andalucía")
                .adminArea2("Málaga")
                .sortBy(StationSearchSortBy.PRICE)
                .size(50)
                .build();

        assertThatThrownBy(() -> factory.create(parameters))
                .isInstanceOf(InvalidStationSearchRequestException.class)
                .hasMessage("LOCALITY search requires countryCode, adminArea1, adminArea2 and adminArea3");
    }

    @Test
    void shouldRejectLocalityParametersInRadiusSearch() {
        StationSearchParameters parameters = StationSearchParameters.builder()
                .searchMode(StationSearchMode.RADIUS)
                .countryCode("ES")
                .adminArea1("Area 1")
                .adminArea2("Area 2")
                .adminArea3("Locality")
                .latitude(BigDecimal.ONE)
                .longitude(BigDecimal.ONE)
                .radiusMeters(1_000)
                .sortBy(StationSearchSortBy.PRICE)
                .size(50)
                .build();

        assertThatThrownBy(() -> factory.create(parameters))
                .isInstanceOf(InvalidStationSearchRequestException.class)
                .hasMessage("RADIUS and VIEWPORT searches must not contain locality filters");
    }
}
