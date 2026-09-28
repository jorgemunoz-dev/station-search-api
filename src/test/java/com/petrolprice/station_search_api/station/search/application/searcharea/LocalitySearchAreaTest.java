package com.petrolprice.station_search_api.station.search.application.searcharea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class LocalitySearchAreaTest {

    @Test
    void shouldNormalizeCountryAndAdministrativeAreas() {
        LocalitySearchArea area = new LocalitySearchArea("es", "  Andalucía  ", "Málaga", "Ardales");

        assertThat(area.countryCode()).isEqualTo("ES");
        assertThat(area.normalizedAdminArea1()).isEqualTo("andalucia");
        assertThat(area.normalizedAdminArea2()).isEqualTo("malaga");
        assertThat(area.normalizedAdminArea3()).isEqualTo("ardales");
    }

    @Test
    void shouldRejectInvalidCountryCode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LocalitySearchArea("ESP", "Andalucía", "Málaga", "Ardales"))
                .withMessage("countryCode must be a two-letter ISO country code");
    }

    @Test
    void shouldRejectBlankAdministrativeArea() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LocalitySearchArea("ES", " ", "Málaga", "Ardales"))
                .withMessage("adminArea1 is required");
    }
}
