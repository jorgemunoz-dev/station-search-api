package com.petrolprice.station_search_api.integration;

import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.aStationSnapshot;
import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.price;
import static com.petrolprice.station_search_api.station.domain.type.ProductType.DIESEL_A;
import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.jdbc.core.JdbcTemplate;

class FuelPriceStatisticsIT extends IntegrationTestBase {
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
        classify(cheap, "North", "Alpha");
        classify(expensive, "South", "Beta");
        completionService.complete(cheap.completionCommand(2));
    }

    @Test
    void shouldCalculateCountryAndAdministrativeAreaCurrentStatistics() {
        var national = currentStatistics
                .current(new CurrentStatisticsQuery("ES", ProductType.DIESEL_A, GeographicScope.country()))
                .orElseThrow();
        var north = currentStatistics.findAreas("ES", null, "PROVINCE").stream()
                .filter(area -> area.name().equals("North"))
                .findFirst()
                .orElseThrow();
        var south = currentStatistics.findAreas("ES", null, "PROVINCE").stream()
                .filter(area -> area.name().equals("South"))
                .findFirst()
                .orElseThrow();
        var beta = currentStatistics.findAreas("ES", south.id(), "MUNICIPALITY").getFirst();
        var topLevelArea = currentStatistics
                .current(new CurrentStatisticsQuery(
                        "ES", ProductType.DIESEL_A, GeographicScope.administrativeArea(north.id())))
                .orElseThrow();
        var childArea = currentStatistics
                .current(new CurrentStatisticsQuery(
                        "ES", ProductType.DIESEL_A, GeographicScope.administrativeArea(beta.id())))
                .orElseThrow();

        assertThat(national.averagePrice()).isEqualByComparingTo("1.600");
        assertThat(national.minimumPrice()).isEqualByComparingTo("1.400");
        assertThat(national.maximumPrice()).isEqualByComparingTo("1.800");
        assertThat(national.stationCount()).isEqualTo(2);
        assertThat(national.cheapestStation().externalId()).isEqualTo(cheap.externalId());
        assertThat(national.mostExpensiveStation().externalId()).isEqualTo(expensive.externalId());
        assertThat(topLevelArea.countryAverageDifference()).isEqualByComparingTo("-0.200");
        assertThat(childArea.parentAreaAverageDifference()).isEqualByComparingTo("0.000");
    }

    @Test
    void shouldDiscoverAndQueryAdministrativeAreasByStableIdentity() {
        var provinces = currentStatistics.findAreas("ES", null, "province");
        assertThat(provinces).extracting(area -> area.name()).containsExactly("North", "South");

        var south = provinces.stream().filter(area -> area.name().equals("South")).findFirst().orElseThrow();
        var municipalities = currentStatistics.findAreas("ES", south.id(), null);
        assertThat(municipalities).singleElement().satisfies(area -> {
            assertThat(area.name()).isEqualTo("Beta");
            assertThat(area.type()).isEqualTo("MUNICIPALITY");
            assertThat(area.parentId()).isEqualTo(south.id());
        });

        var areaStatistics = currentStatistics
                .current(new CurrentStatisticsQuery(
                        "ES", ProductType.DIESEL_A, GeographicScope.administrativeArea(municipalities.getFirst().id())))
                .orElseThrow();
        assertThat(areaStatistics.averagePrice()).isEqualByComparingTo("1.800");
        assertThat(areaStatistics.scope().areaId()).isEqualTo(municipalities.getFirst().id());
        assertThat(areaStatistics.scope().name()).isEqualTo("Beta");

        var rankedChildren = currentStatistics.areas("ES", ProductType.DIESEL_A, south.id(), "municipality");
        assertThat(rankedChildren).singleElement().satisfies(result -> {
            assertThat(result.areaId()).isEqualTo(municipalities.getFirst().id());
            assertThat(result.area()).isEqualTo("Beta");
            assertThat(result.areaType()).isEqualTo("MUNICIPALITY");
        });
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
    }

    private void classify(StationSnapshotFixture station, String province, String municipality) {
        jdbcTemplate.update(
                "UPDATE station SET province = ?, municipality = ? WHERE external_id = ?",
                province,
                municipality,
                station.externalId());
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
