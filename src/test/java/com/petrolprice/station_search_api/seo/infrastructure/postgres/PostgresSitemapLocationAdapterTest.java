package com.petrolprice.station_search_api.seo.infrastructure.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostgresSitemapLocationAdapterTest {
    @Test
    void queryUsesLocalitiesAsSourceAndFiltersOutRowsWithoutStationsOrStatistics() {
        String sql = PostgresSitemapLocationAdapter.FIND_LOCALITIES_WITH_CONTENT;

        assertThat(sql)
                .contains("FROM search_location sl")
                .contains("JOIN station s")
                .contains("JOIN current_fuel_price_statistics fps")
                .contains("fps.station_count > 0")
                .doesNotContain("LEFT JOIN station")
                .doesNotContain("LEFT JOIN current_fuel_price_statistics");
    }

    @Test
    void queryDeduplicatesUsingTheRequiredKey() {
        assertThat(PostgresSitemapLocationAdapter.FIND_LOCALITIES_WITH_CONTENT)
                .contains("GROUP BY country_code, admin_area_1_name, admin_area_2_name, normalized_locality_name");
    }
}
