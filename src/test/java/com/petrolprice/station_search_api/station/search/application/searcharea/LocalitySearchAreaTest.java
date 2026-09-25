package com.petrolprice.station_search_api.station.search.application.searcharea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class LocalitySearchAreaTest {

    @Test
    void shouldNormalizeCountryAndLocality() {
        LocalitySearchArea area = new LocalitySearchArea("es", "  Málaga  ");

        assertThat(area.countryCode()).isEqualTo("ES");
        assertThat(area.normalizedLocality()).isEqualTo("malaga");
    }

    @Test
    void shouldRejectInvalidCountryCode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LocalitySearchArea("ESP", "Málaga"))
                .withMessage("countryCode must be a two-letter ISO country code");
    }

    @Test
    void shouldRejectBlankLocality() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LocalitySearchArea("ES", " "))
                .withMessage("locality is required");
    }
}
