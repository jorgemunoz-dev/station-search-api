package com.petrolprice.station_search_api.statistics.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GeographicScopeTest {
    @Test
    void shouldSelectACompleteAdministrativeHierarchy() {
        GeographicScope scope = GeographicScope.administrativeHierarchy(" Andalucía ", " Málaga ", " Ardales ");

        assertThat(scope.adminArea1Name()).isEqualTo("Andalucía");
        assertThat(scope.adminArea2Name()).isEqualTo("Málaga");
        assertThat(scope.adminArea3Name()).isEqualTo("Ardales");
    }

    @Test
    void shouldSelectCountryWhenNoAdministrativeAreaIsProvided() {
        assertThat(GeographicScope.administrativeHierarchy(null, " ", null)).isEqualTo(GeographicScope.country());
    }

    @Test
    void shouldRejectAdministrativeHierarchyGaps() {
        assertThatThrownBy(() -> GeographicScope.administrativeHierarchy(null, "Málaga", "Ardales"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Administrative areas must be supplied in order: adminArea1, adminArea2, adminArea3");
    }
}
