package com.petrolprice.station_search_api.statistics.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GeographicScopeTest {
    @Test
    void shouldNormalizeLocalityNamesLikeLocationSearch() {
        GeographicScope scope = GeographicScope.locality("  Vélez-Málaga  ");

        assertThat(scope.normalizedLocalityName()).isEqualTo("velez malaga");
    }

    @Test
    void shouldRejectAnEmptyLocality() {
        assertThatThrownBy(() -> GeographicScope.locality("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("locality is required");
    }

    @Test
    void shouldSelectCountrySpecificAdministrativePositionsWithoutEnums() {
        GeographicScope scope = GeographicScope.adminArea2("  Málaga  ");

        assertThat(scope.adminArea2Name()).isEqualTo("Málaga");
        assertThat(scope.normalizedLocalityName()).isNull();
    }
}
