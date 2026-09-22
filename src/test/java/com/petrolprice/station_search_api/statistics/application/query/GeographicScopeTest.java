package com.petrolprice.station_search_api.statistics.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.petrolprice.station_search_api.statistics.domain.GeographicLevel;
import org.junit.jupiter.api.Test;

class GeographicScopeTest {

    @Test
    void shouldCreateLocalityScope() {
        GeographicScope scope = GeographicScope.locality("  Malaga  ", "  Marbella  ");

        assertThat(scope.level()).isEqualTo(GeographicLevel.LOCALITY);
        assertThat(scope.name()).isEqualTo("Marbella");
        assertThat(scope.province()).isEqualTo("Malaga");
    }

    @Test
    void shouldRequireProvinceForLocalityScope() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> GeographicScope.locality(null, "Marbella"))
                .withMessage("province is required for a municipality or locality scope");
    }
}
