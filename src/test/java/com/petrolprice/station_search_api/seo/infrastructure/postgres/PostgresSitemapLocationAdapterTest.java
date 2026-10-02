package com.petrolprice.station_search_api.seo.infrastructure.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostgresSitemapLocationAdapterTest {
    @Test
    void queryUsesCanonicalLocalityIdsForStationsAndStatistics() {
        String sql = PostgresSitemapLocationAdapter.FIND_LOCALITIES_WITH_CONTENT;

        assertThat(sql)
                .contains("JOIN geographic_area locality ON locality.id=s.locality_id")
                .contains("locality.id=fps.area_id")
                .contains("fps.station_count > 0")
                .doesNotContain("REGEXP_REPLACE")
                .doesNotContain("normalized_locality_name =");
    }

    @Test
    void queryDeduplicatesUsingThePublicSitemapKey() {
        assertThat(PostgresSitemapLocationAdapter.FIND_LOCALITIES_WITH_CONTENT)
                .contains("GROUP BY country_code, admin_area_1_name, admin_area_2_name, normalized_locality_name");
    }
}
