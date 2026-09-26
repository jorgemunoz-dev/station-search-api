package com.petrolprice.station_search_api.integration;

import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.aStationSnapshot;
import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.openingPeriod;
import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.price;
import static com.petrolprice.station_search_api.station.domain.type.Day.MON;
import static com.petrolprice.station_search_api.station.domain.type.Day.SAT;
import static com.petrolprice.station_search_api.station.domain.type.Day.TUE;
import static com.petrolprice.station_search_api.station.domain.type.Day.WED;
import static com.petrolprice.station_search_api.station.domain.type.ProductType.DIESEL_A;
import static com.petrolprice.station_search_api.station.domain.type.ProductType.GASOLINE_95_E5;
import static org.assertj.core.api.Assertions.assertThat;

import com.petrolprice.station_search_api.integration.support.StationImportProbe;
import com.petrolprice.station_search_api.integration.support.StationSnapshotFixture;
import com.petrolprice.station_search_api.station.domain.type.ProductType;
import com.petrolprice.station_search_api.station.infrastructure.postgres.PostgresStationSearchPersistenceAdapter;
import com.petrolprice.station_search_api.station.ingestion.application.ProcessStationSnapshotService;
import com.petrolprice.station_search_api.station.search.application.query.FindStationsPageRequest;
import com.petrolprice.station_search_api.station.search.application.query.FindStationsQuery;
import com.petrolprice.station_search_api.station.search.application.query.FindStationsSort;
import com.petrolprice.station_search_api.station.search.application.result.FindStationsItem;
import com.petrolprice.station_search_api.station.search.application.result.FindStationsResult;
import com.petrolprice.station_search_api.station.search.application.searcharea.LocalitySearchArea;
import com.petrolprice.station_search_api.station.search.application.searcharea.RadiusSearchArea;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgresStationSearchPersistenceAdapterIT extends IntegrationTestBase {
    @Autowired
    private PostgresStationSearchPersistenceAdapter adapter;

    @Autowired
    private ProcessStationSnapshotService snapshotService;

    @Autowired
    private StationImportProbe probe;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private StationSnapshotFixture cheapestDiesel;
    private StationSnapshotFixture expensiveDiesel;
    private StationSnapshotFixture gasolineOnly;
    private StationSnapshotFixture outOfRange;
    private StationSnapshotFixture withoutPrices;
    private StationSnapshotFixture middleDiesel;

    @BeforeEach
    void createDynamicSearchScenario() {
        probe.clean();

        StationSnapshotFixture origin = aStationSnapshot();
        latitude = origin.latitude();
        longitude = origin.longitude();

        cheapestDiesel = origin.withPrices(price(DIESEL_A, "1.400"), price(GASOLINE_95_E5, "1.600"))
                .withOpeningPeriods(
                        openingPeriod(List.of(MON, TUE, WED), LocalTime.of(8, 0), LocalTime.of(22, 0)),
                        openingPeriod(List.of(SAT), LocalTime.of(9, 0), LocalTime.of(20, 0)));
        expensiveDiesel = nearby("0.0003").withPrices(price(DIESEL_A, "1.700"), price(GASOLINE_95_E5, "1.500"));
        gasolineOnly = nearby("0.0006").withPrices(price(GASOLINE_95_E5, "1.300"));
        outOfRange = nearby("0.05").withPrices(price(DIESEL_A, "1.100"));
        withoutPrices = nearby("0.0002").withPrices();
        middleDiesel = nearby("0.0009").withPrices(price(DIESEL_A, "1.550"));

        List.of(cheapestDiesel, expensiveDiesel, gasolineOnly, outOfRange, withoutPrices, middleDiesel)
                .forEach(station -> snapshotService.consume(station.processCommand()));
    }

    @Test
    void shouldFilterStationsByRadius() {
        FindStationsResult result = adapter.search(query(null, FindStationsSort.DISTANCE, 10));

        assertThat(externalIds(result))
                .containsExactlyInAnyOrder(
                        cheapestDiesel.externalId(),
                        expensiveDiesel.externalId(),
                        gasolineOnly.externalId(),
                        withoutPrices.externalId(),
                        middleDiesel.externalId())
                .doesNotContain(outOfRange.externalId());
    }

    @Test
    void shouldFilterStationsByProductType() {
        FindStationsResult result = adapter.search(query(DIESEL_A, FindStationsSort.DISTANCE, 10));

        assertThat(externalIds(result))
                .containsExactlyInAnyOrder(
                        cheapestDiesel.externalId(), expensiveDiesel.externalId(), middleDiesel.externalId());
    }

    @Test
    void shouldSortStationsByPrice() {
        FindStationsResult result = adapter.search(query(DIESEL_A, FindStationsSort.PRICE, 10));

        assertThat(externalIds(result))
                .containsExactly(cheapestDiesel.externalId(), middleDiesel.externalId(), expensiveDiesel.externalId());
    }

    @Test
    void shouldRespectLimitAfterFilteringAndSorting() {
        FindStationsResult result = adapter.search(query(DIESEL_A, FindStationsSort.PRICE, 2));

        assertThat(externalIds(result)).containsExactly(cheapestDiesel.externalId(), middleDiesel.externalId());
        assertThat(result.page().hasNext()).isTrue();
    }

    @Test
    void shouldLoadStationDetailsWithoutDuplicates() {
        FindStationsResult result = adapter.search(query(DIESEL_A, FindStationsSort.PRICE, 10));

        assertThat(externalIds(result)).containsOnlyOnce(cheapestDiesel.externalId());

        FindStationsItem station = result.items().stream()
                .filter(item -> item.station().getExternalId().equals(cheapestDiesel.externalId()))
                .findFirst()
                .orElseThrow();

        assertThat(station.station().getProductPrices())
                .extracting(productPrice -> productPrice.getProductType())
                .containsExactlyInAnyOrder(DIESEL_A, GASOLINE_95_E5);
        assertThat(station.station().getOpeningPeriods()).hasSize(2).anySatisfy(period -> {
            assertThat(period.getDays()).containsExactlyInAnyOrder(MON, TUE, WED);
            assertThat(period.getOpen()).isEqualTo(LocalTime.of(8, 0));
            assertThat(period.getClose()).isEqualTo(LocalTime.of(22, 0));
        });
    }

    @Test
    void shouldFilterStationsByExactNormalizedLocality() {
        StationSnapshotFixture malaga = aStationSnapshot().withLocality("Málaga");
        StationSnapshotFixture neighboringLocality = aStationSnapshot().withLocality("Málaga del Fresno");
        snapshotService.consume(malaga.processCommand());
        snapshotService.consume(neighboringLocality.processCommand());

        FindStationsQuery query = FindStationsQuery.builder()
                .searchArea(new LocalitySearchArea("es", "malaga"))
                .sortBy(FindStationsSort.PRICE)
                .pageRequest(FindStationsPageRequest.builder().size(10).build())
                .build();

        assertThat(externalIds(adapter.search(query)))
                .containsExactly(malaga.externalId())
                .doesNotContain(neighboringLocality.externalId());
    }

    @Test
    void shouldMatchLocalityWhenMunicipalityUsesABilingualCompositeName() {
        StationSnapshotFixture castellon = aStationSnapshot()
                .withLocalityAndMunicipality(
                        "CASTELLON DE LA PLANA", "Castellón de la Plana/Castelló de la Plana")
                .withPrices(price(GASOLINE_95_E5, "1.819"));
        snapshotService.consume(castellon.processCommand());

        FindStationsQuery query = FindStationsQuery.builder()
                .searchArea(new LocalitySearchArea("ES", "Castellón de la Plana"))
                .productType(GASOLINE_95_E5)
                .sortBy(FindStationsSort.PRICE)
                .pageRequest(FindStationsPageRequest.builder().size(100).build())
                .build();

        assertThat(externalIds(adapter.search(query))).containsExactly(castellon.externalId());
    }

    @Test
    void shouldResolveAllUnambiguousPostalCodesForAGeoNamesLocality() {
        StationSnapshotFixture firstPostalCode = stationIn("15001", "CORUÑA (A)");
        StationSnapshotFixture secondPostalCode = stationIn("15002", "CORUÑA (A)");
        StationSnapshotFixture ambiguousPostalCode = stationIn("15003", "CORUÑA (A)");
        StationSnapshotFixture otherLocality = stationIn("15003", "OLEIROS");
        List.of(firstPostalCode, secondPostalCode, ambiguousPostalCode, otherLocality)
                .forEach(station -> snapshotService.consume(station.processCommand()));
        insertSearchLocation("15001", "A Coruña", "a coruna");
        insertSearchLocation("15002", "A Coruña", "a coruna");
        insertSearchLocation("15003", "A Coruña", "a coruna");

        FindStationsQuery query = FindStationsQuery.builder()
                .searchArea(new LocalitySearchArea("ES", "a coruna"))
                .sortBy(FindStationsSort.PRICE)
                .pageRequest(FindStationsPageRequest.builder().size(100).build())
                .build();

        assertThat(externalIds(adapter.search(query)))
                .containsExactlyInAnyOrder(firstPostalCode.externalId(), secondPostalCode.externalId())
                .doesNotContain(ambiguousPostalCode.externalId(), otherLocality.externalId());
    }

    private StationSnapshotFixture stationIn(String postalCode, String stationLocality) {
        return aStationSnapshot()
                .withPostalCode(postalCode)
                .withLocalityAndMunicipality(stationLocality, stationLocality);
    }

    private void insertSearchLocation(String postalCode, String localityName, String normalizedLocalityName) {
        jdbcTemplate.update(
                """
            INSERT INTO search_location (
                id, country_code, postal_code, normalized_postal_code,
                locality_name, normalized_locality_name, location, source
            ) VALUES (
                gen_random_uuid(), 'ES', ?, ?, ?, ?,
                ST_SetSRID(ST_MakePoint(-8.4, 43.3), 4326)::geography, 'GEONAMES'
            )
            """,
                postalCode,
                postalCode,
                localityName,
                normalizedLocalityName);
    }

    private StationSnapshotFixture nearby(String offset) {
        BigDecimal delta = new BigDecimal(offset);
        return aStationSnapshot().withLocation(latitude.add(delta), longitude.add(delta));
    }

    private FindStationsQuery query(ProductType productType, FindStationsSort sort, int size) {
        return FindStationsQuery.builder()
                .searchArea(new RadiusSearchArea(latitude, longitude, 500))
                .productType(productType)
                .sortBy(sort)
                .pageRequest(FindStationsPageRequest.builder().size(size).build())
                .build();
    }

    private List<String> externalIds(FindStationsResult result) {
        return result.items().stream()
                .map(item -> item.station().getExternalId())
                .toList();
    }
}
