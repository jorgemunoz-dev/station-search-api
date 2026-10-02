package com.petrolprice.station_search_api.station.infrastructure.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostgresGeographicAreaResolverTest {
    @Test
    void prioritizesAnUnambiguousPostalCandidateBeforeNameFallbacks() {
        assertThat(PostgresGeographicAreaResolver.SQL)
                .contains("postal_candidate_count=1")
                .contains("locality_name=:locality")
                .contains(":locality=ANY(ga.aliases)")
                .contains("similarity(locality_name,:locality)>=0.85");
    }

    @Test
    void acceptsSourcesThatRepresentTheLocalityAsAdminAreaThree() {
        assertThat(PostgresGeographicAreaResolver.SQL)
                .contains("admin3_name=:admin3")
                .contains("admin3_name IS NULL AND locality_name=:admin3");
    }
}
