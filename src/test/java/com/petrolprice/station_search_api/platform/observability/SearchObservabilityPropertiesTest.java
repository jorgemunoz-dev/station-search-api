package com.petrolprice.station_search_api.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchObservabilityPropertiesTest {

    @Test
    void appliesSafeDefaults() {
        SearchObservabilityProperties properties = new SearchObservabilityProperties(null, null);

        assertThat(properties.paths()).isEmpty();
        assertThat(properties.slowQueryThreshold()).isEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void keepsConfiguredValues() {
        SearchObservabilityProperties properties =
                new SearchObservabilityProperties(List.of("/stations"), Duration.ofMillis(500));

        assertThat(properties.paths()).containsExactly("/stations");
        assertThat(properties.slowQueryThreshold()).isEqualTo(Duration.ofMillis(500));
    }

    @Test
    void rejectsNonPositiveThreshold() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SearchObservabilityProperties(List.of(), Duration.ZERO))
                .withMessage("slowQueryThreshold must be greater than zero");
    }
}
