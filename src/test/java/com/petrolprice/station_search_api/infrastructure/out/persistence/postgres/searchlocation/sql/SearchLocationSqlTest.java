package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.searchlocation.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petrolprice.station_search_api.application.usecase.searchlocation.query.SearchLocationQuery;
import org.junit.jupiter.api.Test;

class SearchLocationSqlTest {
    @Test
    void shouldNormalizeInputValues() {
        SearchLocationQuery query = new SearchLocationQuery("  Málaga  ", "es", 10);

        assertThat(query.query()).isEqualTo("Málaga");
        assertThat(query.countryCode()).isEqualTo("ES");
        assertThat(query.limit()).isEqualTo(10);
    }

    @Test
    void shouldRejectBlankQuery() {
        assertThatThrownBy(() -> new SearchLocationQuery(" ", "ES", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Search query cannot be empty");
    }

    @Test
    void shouldRejectInvalidCountryCode() {
        assertThatThrownBy(() -> new SearchLocationQuery("Málaga", "ESP", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Country code must contain exactly two characters");
    }

    @Test
    void shouldRejectLimitGreaterThanTwenty() {
        assertThatThrownBy(() -> new SearchLocationQuery("Málaga", "ES", 21))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Limit must be between 1 and 20");
    }
}
