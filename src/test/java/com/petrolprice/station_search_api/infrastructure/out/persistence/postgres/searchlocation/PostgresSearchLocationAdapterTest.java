package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.searchlocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petrolprice.station_search_api.application.port.out.SearchLocationPort;
import com.petrolprice.station_search_api.application.usecase.searchlocation.SearchLocationType;
import com.petrolprice.station_search_api.application.usecase.searchlocation.SearchLocationsService;
import com.petrolprice.station_search_api.application.usecase.searchlocation.query.SearchLocationQuery;
import com.petrolprice.station_search_api.application.usecase.searchlocation.result.SearchLocationResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresSearchLocationAdapterTest {
    @Mock
    private SearchLocationPort searchLocationPort;

    private SearchLocationsService service;

    @BeforeEach
    void setUp() {
        service = new SearchLocationsService(searchLocationPort);
    }

    @Test
    void shouldReturnLocationsProvidedByPort() {
        SearchLocationQuery query = new SearchLocationQuery("mal", "ES", 10);

        SearchLocationResult expected = new SearchLocationResult(
                SearchLocationType.LOCALITY, "Málaga", "Málaga, Andalucía", "ES", null, 36.7213, -4.4214);

        when(searchLocationPort.search(query)).thenReturn(List.of(expected));

        List<SearchLocationResult> result = service.search(query);

        assertThat(result).containsExactly(expected);

        verify(searchLocationPort).search(query);
    }

    @Test
    void shouldReturnEmptyListWhenNoLocationsMatch() {
        SearchLocationQuery query = new SearchLocationQuery("unknown", "ES", 10);

        when(searchLocationPort.search(query)).thenReturn(List.of());

        List<SearchLocationResult> result = service.search(query);

        assertThat(result).isEmpty();

        verify(searchLocationPort).search(query);
    }
}
