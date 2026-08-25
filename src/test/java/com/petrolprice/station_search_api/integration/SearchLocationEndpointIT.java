package com.petrolprice.station_search_api.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class SearchLocationEndpointIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                """
            INSERT INTO search_location (
                id,
                country_code,
                postal_code,
                normalized_postal_code,
                locality_name,
                normalized_locality_name,
                admin_area_1_name,
                admin_area_1_code,
                admin_area_2_name,
                admin_area_2_code,
                location,
                accuracy,
                source
            )
            VALUES (
                '11111111-1111-1111-1111-111111111111',
                'ES',
                '29550',
                '29550',
                'Ardales',
                'ardales',
                'Andalucía',
                '01',
                'Málaga',
                '29',
                ST_SetSRID(
                    ST_MakePoint(-4.8460, 36.8780),
                    4326
                )::geography,
                6,
                'GEONAMES'
            )
            """);
    }

    @Test
    void shouldSearchLocationsByLocality() throws Exception {
        mockMvc.perform(get("/locations/search")
                        .queryParam("query", "ard")
                        .queryParam("countryCode", "ES")
                        .queryParam("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("LOCALITY")))
                .andExpect(jsonPath("$[0].primaryText", is("Ardales")))
                .andExpect(jsonPath("$[0].countryCode", is("ES")))
                .andExpect(jsonPath("$[0].latitude", is(36.8780)))
                .andExpect(jsonPath("$[0].longitude", is(-4.8460)));
    }

    @Test
    void shouldSearchLocationsByPostalCode() throws Exception {
        mockMvc.perform(get("/locations/search")
                        .queryParam("query", "295")
                        .queryParam("countryCode", "ES")
                        .queryParam("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type", is("POSTAL_CODE")))
                .andExpect(jsonPath("$[0].postalCode", is("29550")));
    }

    @Test
    void shouldReturnBadRequestWhenQueryIsMissing() throws Exception {
        mockMvc.perform(get("/locations/search").queryParam("countryCode", "ES"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenQueryIsTooShort() throws Exception {
        mockMvc.perform(get("/locations/search").queryParam("query", "a").queryParam("countryCode", "ES"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForInvalidCountryCode() throws Exception {
        mockMvc.perform(get("/locations/search").queryParam("query", "ard").queryParam("countryCode", "ESP"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenLimitExceedsMaximum() throws Exception {
        mockMvc.perform(get("/locations/search")
                        .queryParam("query", "ard")
                        .queryParam("countryCode", "ES")
                        .queryParam("limit", "21"))
                .andExpect(status().isBadRequest());
    }
}
