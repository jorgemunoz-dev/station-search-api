package com.petrolprice.station_search_api.station.search.application.searcharea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class AdministrativeSearchAreaTest {

    @Test
    void shouldNormalizeNameAndCountryCode() {
        AdministrativeSearchArea area = new AdministrativeSearchArea(
                AdministrativeSearchArea.Type.LOCALITY, "  Malaga  ", "es");

        assertThat(area.name()).isEqualTo("Malaga");
        assertThat(area.countryCode()).isEqualTo("ES");
    }

    @Test
    void shouldRejectBlankName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AdministrativeSearchArea(
                        AdministrativeSearchArea.Type.MUNICIPALITY, "  ", "ES"))
                .withMessage("Administrative area name is required");
    }

    @Test
    void shouldRejectInvalidCountryCode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AdministrativeSearchArea(
                        AdministrativeSearchArea.Type.PROVINCE, "Malaga", "ESP"))
                .withMessage("countryCode must contain two letters");
    }
}
