package com.petrolprice.station_search_api.integration;

import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.aStationSnapshot;
import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.price;
import static com.petrolprice.station_search_api.station.domain.type.ProductType.DIESEL_A;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petrolprice.station_search_api.integration.support.StationImportProbe;
import com.petrolprice.station_search_api.integration.support.StationSnapshotFixture;
import com.petrolprice.station_search_api.station.ingestion.application.CompleteStationPublishingService;
import com.petrolprice.station_search_api.station.ingestion.application.ProcessStationSnapshotService;
import com.petrolprice.station_search_api.statistics.application.port.out.CurrentPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.port.out.HistoricalPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.port.out.StatisticsCalculationRepository;
import com.petrolprice.station_search_api.statistics.application.query.CurrentStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.GeographicScope;
import com.petrolprice.station_search_api.statistics.application.query.HistoricalStatisticsQuery;
import com.petrolprice.station_search_api.statistics.domain.ProductType;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class FuelPriceStatisticsIT extends IntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProcessStationSnapshotService snapshotService;

    @Autowired
    private CompleteStationPublishingService completionService;

    @Autowired
    private CurrentPriceStatisticsRepository currentStatistics;

    @Autowired
    private HistoricalPriceStatisticsRepository historicalStatistics;

    @Autowired
    private StatisticsCalculationRepository calculationRepository;

    @Autowired
    private StationImportProbe probe;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private StationSnapshotFixture cheap;
    private StationSnapshotFixture expensive;

    @BeforeEach
    void setUp() {
        probe.clean();
        UUID snapshotId = UUID.randomUUID();
        cheap = aStationSnapshot()
                .withSnapshotId(snapshotId)
                .withLocation("40.0000", "-3.0000")
                .withPrices(price(DIESEL_A, "1.400"));
        expensive = aStationSnapshot()
                .withSnapshotId(snapshotId)
                .withLocation("40.0100", "-3.0100")
                .withPrices(price(DIESEL_A, "1.800"));
        snapshotService.consume(cheap.processCommand());
        snapshotService.consume(expensive.processCommand());
        classify(cheap, "10001", "North", "Alpha", "alpha");
        classify(expensive, "20001", "South", "Beta", "beta");
        completionService.complete(cheap.completionCommand(2));
    }

    @Test
    void shouldCalculateCountryAndLocalityCurrentStatistics() {
        var country = currentStatistics
                .current(new CurrentStatisticsQuery("ES", ProductType.DIESEL_A, GeographicScope.country()))
                .orElseThrow();
        var alpha = currentStatistics
                .current(new CurrentStatisticsQuery(
                        "ES", ProductType.DIESEL_A, GeographicScope.locality("Álpha")))
                .orElseThrow();
        var beta = currentStatistics
                .current(new CurrentStatisticsQuery(
                        "ES", ProductType.DIESEL_A, GeographicScope.locality("Beta")))
                .orElseThrow();
        var south = currentStatistics
                .current(new CurrentStatisticsQuery(
                        "ES", ProductType.DIESEL_A, GeographicScope.adminArea2("south")))
                .orElseThrow();

        assertThat(country.averagePrice()).isEqualByComparingTo("1.600");
        assertThat(country.minimumPrice()).isEqualByComparingTo("1.400");
        assertThat(country.maximumPrice()).isEqualByComparingTo("1.800");
        assertThat(country.stationCount()).isEqualTo(2);
        assertThat(country.cheapestStation().externalId()).isEqualTo(cheap.externalId());
        assertThat(country.mostExpensiveStation().externalId()).isEqualTo(expensive.externalId());
        assertThat(alpha.countryAverageDifference()).isEqualByComparingTo("-0.200");
        assertThat(beta.scope().adminArea2Name()).isEqualTo("South");
        assertThat(beta.adminArea2AverageDifference()).isEqualByComparingTo("0.000");
        assertThat(south.averagePrice()).isEqualByComparingTo("1.800");
    }

    @Test
    void shouldRankLocalitiesFilteredByAdministrativeContext() {
        var ranked = currentStatistics.localities("ES", ProductType.DIESEL_A, null, "South", null);
        assertThat(ranked).singleElement().satisfies(result -> {
            assertThat(result.normalizedLocalityName()).isEqualTo("beta");
            assertThat(result.localityName()).isEqualTo("Beta");
            assertThat(result.adminArea2Name()).isEqualTo("South");
        });
    }

    @Test
    void shouldExposeCurrentCountryLocalityAndAdminAreaStatistics() throws Exception {
        mockMvc.perform(get("/statistics/fuel-prices/current")
                .queryParam("countryCode", "ES")
                        .queryParam("productType", "DIESEL_A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationCount", is(2)));

        mockMvc.perform(get("/statistics/fuel-prices/current")
                        .queryParam("countryCode", "ES")
                        .queryParam("productType", "DIESEL_A")
                        .queryParam("locality", "Béta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationCount", is(1)))
                .andExpect(jsonPath("$.scope.normalizedLocalityName", is("beta")))
                .andExpect(jsonPath("$.scope.adminArea2Name", is("South")));

        mockMvc.perform(get("/statistics/fuel-prices/current")
                        .queryParam("countryCode", "ES")
                        .queryParam("productType", "DIESEL_A")
                        .queryParam("adminArea2", "south"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationCount", is(1)))
                .andExpect(jsonPath("$.scope.adminArea2Name", is("South")));
    }

    @Test
    void shouldExposeLocalityRankingsWithNullableAdminFilters() throws Exception {
        mockMvc.perform(get("/statistics/fuel-prices/localities")
                        .queryParam("countryCode", "ES")
                        .queryParam("productType", "DIESEL_A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/statistics/fuel-prices/localities")
                        .queryParam("countryCode", "ES")
                        .queryParam("productType", "DIESEL_A")
                        .queryParam("adminArea2", "SOUTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].normalizedLocalityName", is("beta")));
    }

    @Test
    void shouldExposeAdminAreaHistoryAndRejectAmbiguousScopes() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        mockMvc.perform(get("/statistics/fuel-prices/history")
                        .queryParam("countryCode", "ES")
                        .queryParam("productType", "DIESEL_A")
                        .queryParam("adminArea2", "South")
                        .queryParam("from", today.toString())
                        .queryParam("to", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].stationCount", is(1)));

        mockMvc.perform(get("/statistics/fuel-prices/current")
                        .queryParam("countryCode", "ES")
                        .queryParam("productType", "DIESEL_A")
                        .queryParam("locality", "Beta")
                        .queryParam("adminArea2", "South"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_STATISTICS_QUERY")));
    }

    @Test
    void shouldReadHistoricalStatisticsCalculatedForCompletedSnapshots() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        UUID stationId = stationId(cheap.externalId());
        createCalculatedSnapshot(
                stationId, "1.600", today.minusDays(8).atTime(18, 0).toInstant(ZoneOffset.UTC));
        createCalculatedSnapshot(
                stationId, "1.700", today.minusDays(1).atTime(8, 0).toInstant(ZoneOffset.UTC));

        var history = historicalStatistics.history(new HistoricalStatisticsQuery(
                "ES", ProductType.DIESEL_A, GeographicScope.country(), today.minusDays(8), today.minusDays(1)));

        assertThat(history).hasSize(2);
        assertThat(history.getFirst().averagePrice()).isEqualByComparingTo("1.600");
        assertThat(history.getFirst().periodAveragePrice()).isEqualByComparingTo("1.650");
        assertThat(history.getLast().changeFromSevenDaysAgo()).isEqualByComparingTo("0.100");

        var adminAreaHistory = historicalStatistics.history(new HistoricalStatisticsQuery(
                "ES",
                ProductType.DIESEL_A,
                GeographicScope.adminArea2("North"),
                today.minusDays(8),
                today.minusDays(1)));
        assertThat(adminAreaHistory).hasSize(2);
        assertThat(adminAreaHistory.getLast().averagePrice()).isEqualByComparingTo("1.700");
    }

    private void classify(
            StationSnapshotFixture station,
            String postalCode,
            String adminArea2,
            String locality,
            String normalizedLocality) {
        jdbcTemplate.update(
                "UPDATE station SET postal_code = ?, province = ?, municipality = ? WHERE external_id = ?",
                postalCode,
                adminArea2,
                locality,
                station.externalId());
        jdbcTemplate.update(
                """
                INSERT INTO search_location (
                    id, country_code, postal_code, normalized_postal_code,
                    locality_name, normalized_locality_name, admin_area_2_name,
                    location, accuracy, source, created_at, updated_at
                ) VALUES (?, 'ES', ?, ?, ?, ?, ?, ST_GeogFromText('SRID=4326;POINT(-3 40)'),
                          10, 'TEST', NOW(), NOW())
                ON CONFLICT (country_code, normalized_postal_code, normalized_locality_name) DO NOTHING
                """,
                UUID.randomUUID(),
                postalCode,
                postalCode,
                locality,
                normalizedLocality,
                adminArea2);
    }

    private UUID stationId(String externalId) {
        return jdbcTemplate.queryForObject("SELECT id FROM station WHERE external_id = ?", UUID.class, externalId);
    }

    private void createCalculatedSnapshot(UUID stationId, String value, Instant observedAt) {
        UUID snapshotId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO station_import (
                    snapshot_id, country, status, published_stations, processed_stations,
                    publishing_completed, created_at, updated_at, completed_at
                ) VALUES (?, 'ES', 'COMPLETED', 1, 1, TRUE, ?, ?, ?)
                """,
                snapshotId,
                Timestamp.from(observedAt),
                Timestamp.from(observedAt),
                Timestamp.from(observedAt));
        jdbcTemplate.update(
                """
                INSERT INTO historical_product_price (
                    id, snapshot_id, station_id, product_type, price, observed_at
                ) VALUES (?, ?, ?, 'DIESEL_A', ?, ?)
                """,
                UUID.randomUUID(),
                snapshotId,
                stationId,
                new BigDecimal(value),
                Timestamp.from(observedAt));
        calculationRepository.replaceForSnapshot(snapshotId);
        jdbcTemplate.update(
                "UPDATE fuel_price_statistics SET calculated_at = ? WHERE snapshot_id = ?",
                Timestamp.from(observedAt),
                snapshotId);
    }
}
