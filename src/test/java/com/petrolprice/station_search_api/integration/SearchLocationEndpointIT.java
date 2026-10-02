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
                WITH country AS (
                  INSERT INTO geographic_area(country_code,type,name,normalized_name,parent_id,source,source_code)
                  VALUES ('ES','COUNTRY','España','espana',NULL,'TEST','ES')
                  ON CONFLICT (country_code) WHERE type='COUNTRY' DO UPDATE SET name=EXCLUDED.name
                  RETURNING id
                ), a1 AS (
                  INSERT INTO geographic_area(country_code,type,name,normalized_name,parent_id,source,source_code)
                  SELECT 'ES','ADMIN_AREA_1','Andalucía','andalucia',id,'TEST','01' FROM country
                  ON CONFLICT (country_code,type,parent_id,normalized_name) DO UPDATE SET name=EXCLUDED.name RETURNING id
                ), a2 AS (
                  INSERT INTO geographic_area(country_code,type,name,normalized_name,parent_id,source,source_code)
                  SELECT 'ES','ADMIN_AREA_2','Málaga','malaga',id,'TEST','29' FROM a1
                  ON CONFLICT (country_code,type,parent_id,normalized_name) DO UPDATE SET name=EXCLUDED.name RETURNING id
                ), locality AS (
                  INSERT INTO geographic_area(country_code,type,name,normalized_name,parent_id,source)
                  SELECT 'ES','LOCALITY','Ardales','ardales',id,'TEST' FROM a2
                  ON CONFLICT (country_code,type,parent_id,normalized_name) DO UPDATE SET name=EXCLUDED.name RETURNING id
                )
                INSERT INTO search_location(id,country_code,postal_code,normalized_postal_code,locality_id,location,accuracy,source)
                SELECT '11111111-1111-1111-1111-111111111111','ES','29550','29550',id,
                  ST_SetSRID(ST_MakePoint(-4.8460,36.8780),4326)::geography,6,'GEONAMES' FROM locality
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
                .andExpect(jsonPath("$[0].postalCode", is("29550")))
                .andExpect(jsonPath("$[0].stationLocalityName", is("Ardales")))
                .andExpect(jsonPath("$[0].normalizedLocalityName", is("ardales")))
                .andExpect(jsonPath("$[0].adminArea1Name", is("Andalucía")))
                .andExpect(jsonPath("$[0].adminArea1Code", is("01")))
                .andExpect(jsonPath("$[0].adminArea2Name", is("Málaga")))
                .andExpect(jsonPath("$[0].adminArea2Code", is("29")))
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
                .andExpect(jsonPath("$[0].postalCode", is("29550")))
                .andExpect(jsonPath("$[0].stationLocalityName", is("Ardales")));
    }

    @Test
    void shouldResolveStationLocalityFromAnUnambiguousPostalCode() throws Exception {
        insertStation("22222222-2222-2222-2222-222222222222", "29550", "ARDales pueblo", "Ardales (El)");
        insertStation("33333333-3333-3333-3333-333333333333", "29550", "Another locality", "Ardales (El)");
        insertSearchLocationWithoutStations();

        mockMvc.perform(get("/locations/search")
                        .queryParam("query", "ard")
                        .queryParam("countryCode", "ES")
                        .queryParam("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].primaryText", is("Ardales")))
                .andExpect(jsonPath("$[0].stationLocalityName", is("Ardales")));
    }

    @Test
    void shouldKeepGeoNamesLocalityWhenPostalCodeIsAmbiguous() throws Exception {
        insertStation("22222222-2222-2222-2222-222222222222", "29550", "Ardales", "Ardales (El)");
        insertStation("33333333-3333-3333-3333-333333333333", "29550", "Carratraca", "Carratraca");

        mockMvc.perform(get("/locations/search")
                        .queryParam("query", "ard")
                        .queryParam("countryCode", "ES")
                        .queryParam("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stationLocalityName", is("Ardales")));
    }

    @Test
    void shouldReturnBadRequestWhenQueryIsMissing() throws Exception {
        mockMvc.perform(get("/locations/search").queryParam("countryCode", "ES"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCountryCodeIsMissing() throws Exception {
        mockMvc.perform(get("/locations/search").queryParam("query", "ard"))
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

    private void insertStation(String id, String postalCode, String localityName, String normalizedLocalityName) {
        jdbcTemplate.update(
                """
            INSERT INTO station (
                id, external_id, country, postal_code, locality_id, admin_area_1_id, admin_area_2_id,
                location, created_at, updated_at
            ) SELECT ?::uuid, ?, 'ES', ?, locality.id, a1.id, a2.id,
                ST_SetSRID(ST_MakePoint(-4.8460, 36.8780), 4326)::geography, NOW(), NOW()
              FROM geographic_area locality
              JOIN geographic_area a2 ON a2.id=locality.parent_id
              JOIN geographic_area a1 ON a1.id=a2.parent_id
              WHERE locality.country_code='ES' AND locality.type='LOCALITY' AND locality.normalized_name='ardales'
            """,
                id,
                id,
                postalCode);
    }

    private void insertSearchLocationWithoutStations() {
        jdbcTemplate.update(
                """
            INSERT INTO search_location (
                id, country_code, postal_code, normalized_postal_code, locality_id, location, accuracy, source
            ) SELECT '44444444-4444-4444-4444-444444444444', 'ES', '29551', '29551', id,
                ST_SetSRID(ST_MakePoint(-4.8460, 36.8780), 4326)::geography, 10, 'GEONAMES'
              FROM geographic_area WHERE country_code='ES' AND type='LOCALITY' AND normalized_name='ardales'
            """);
    }
}
