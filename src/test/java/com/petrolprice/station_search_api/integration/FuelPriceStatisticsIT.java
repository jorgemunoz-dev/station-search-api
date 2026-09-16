package com.petrolprice.station_search_api.integration;

import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.aStationSnapshot;
import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.price;
import static com.petrolprice.station_search_api.station.domain.type.ProductType.DIESEL_A;
import static org.assertj.core.api.Assertions.assertThat;

import com.petrolprice.station_search_api.integration.support.StationImportProbe;
import com.petrolprice.station_search_api.integration.support.StationSnapshotFixture;
import com.petrolprice.station_search_api.station.ingestion.application.ProcessStationSnapshotService;
import com.petrolprice.station_search_api.statistics.application.port.out.CurrentPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.port.out.GeospatialPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.port.out.HistoricalPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.query.CurrentStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.GeographicScope;
import com.petrolprice.station_search_api.statistics.application.query.HistoricalStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.RadiusStatisticsQuery;
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
    private CurrentPriceStatisticsRepository currentStatistics;

    @Autowired
    private HistoricalPriceStatisticsRepository historicalStatistics;

    @Autowired
    private GeospatialPriceStatisticsRepository geospatialStatistics;

    @Autowired
    private StationImportProbe probe;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private StationSnapshotFixture cheap;
    private StationSnapshotFixture expensive;

    @BeforeEach
    void setUp() {
        probe.clean();
        cheap = aStationSnapshot().withLocation("40.0000", "-3.0000").withPrices(price(DIESEL_A, "1.400"));
        expensive = aStationSnapshot().withLocation("40.0100", "-3.0100").withPrices(price(DIESEL_A, "1.800"));
        snapshotService.consume(cheap.processCommand());
        snapshotService.consume(expensive.processCommand());
        classify(cheap, "North", "Alpha");
        classify(expensive, "South", "Beta");
    }

    @Test
    void shouldCalculateNationalProvinceAndMunicipalityCurrentStatistics() {
        var national = currentStatistics.current(new CurrentStatisticsQuery(
                        "ES", ProductType.DIESEL_A, GeographicScope.national()))
                .orElseThrow();
        var province = currentStatistics.current(new CurrentStatisticsQuery(
                        "ES", ProductType.DIESEL_A, GeographicScope.province("North")))
                .orElseThrow();
        var municipality = currentStatistics.current(new CurrentStatisticsQuery(
                        "ES", ProductType.DIESEL_A, GeographicScope.municipality("South", "Beta")))
                .orElseThrow();

        assertThat(national.averagePrice()).isEqualByComparingTo("1.600");
        assertThat(national.minimumPrice()).isEqualByComparingTo("1.400");
        assertThat(national.maximumPrice()).isEqualByComparingTo("1.800");
        assertThat(national.stationCount()).isEqualTo(2);
        assertThat(national.cheapestStation().externalId()).isEqualTo(cheap.externalId());
        assertThat(national.mostExpensiveStation().externalId()).isEqualTo(expensive.externalId());
        assertThat(province.nationalAverageDifference()).isEqualByComparingTo("-0.200");
        assertThat(municipality.provincialAverageDifference()).isEqualByComparingTo("0.000");
    }

    @Test
    void shouldRankProvincesAndAggregateStationsAroundCoordinates() {
        var provinces = currentStatistics.provinces("ES", ProductType.DIESEL_A);
        var radius = geospatialStatistics.around(new RadiusStatisticsQuery(
                        "ES", ProductType.DIESEL_A, new BigDecimal("40.0000"), new BigDecimal("-3.0000"), 5_000))
                .orElseThrow();

        assertThat(provinces).extracting(item -> item.area()).containsExactly("North", "South");
        assertThat(provinces.getFirst().cheapestRank()).isOne();
        assertThat(radius.stationCount()).isEqualTo(2);
        assertThat(radius.priceSpread()).isEqualByComparingTo("0.400");
        assertThat(radius.cheapestStation().externalId()).isEqualTo(cheap.externalId());
        assertThat(radius.nearestStation().externalId()).isEqualTo(cheap.externalId());
    }

    @Test
    void shouldUseOneHistoricalObservationPerStationAndDayAndCalculateExactVariations() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        UUID stationId = stationId(cheap.externalId());
        insertHistory(stationId, "1.500", today.minusDays(8).atTime(8, 0).toInstant(ZoneOffset.UTC));
        insertHistory(stationId, "1.600", today.minusDays(8).atTime(18, 0).toInstant(ZoneOffset.UTC));
        insertHistory(stationId, "1.700", today.minusDays(1).atTime(8, 0).toInstant(ZoneOffset.UTC));

        var history = historicalStatistics.history(new HistoricalStatisticsQuery(
                "ES", ProductType.DIESEL_A, GeographicScope.national(), today.minusDays(8), today.minusDays(1)));

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

    private void insertHistory(UUID stationId, String value, Instant observedAt) {
        jdbcTemplate.update(
                """
                INSERT INTO historical_product_price (id, station_id, product_type, price, observed_at)
                VALUES (?, ?, 'DIESEL_A', ?, ?)
                """,
                UUID.randomUUID(),
                stationId,
                new BigDecimal(value),
                Timestamp.from(observedAt));
    }
}
