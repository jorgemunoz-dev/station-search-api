package com.petrolprice.station_search_api.integration;

import static com.petrolprice.station_search_api.station.domain.type.Day.MON;
import static com.petrolprice.station_search_api.station.domain.type.Day.SAT;
import static com.petrolprice.station_search_api.station.domain.type.Day.TUE;
import static com.petrolprice.station_search_api.station.domain.type.Day.WED;
import static com.petrolprice.station_search_api.station.domain.type.ProductType.DIESEL_A;
import static com.petrolprice.station_search_api.station.domain.type.ProductType.GASOLINE_95_E5;
import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.aStationSnapshot;
import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.openingPeriod;
import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.price;
import static org.assertj.core.api.Assertions.assertThat;

import com.petrolprice.station_search_api.station.search.application.query.FindStationsPageRequest;
import com.petrolprice.station_search_api.station.search.application.query.FindStationsQuery;
import com.petrolprice.station_search_api.station.search.application.query.FindStationsSort;
import com.petrolprice.station_search_api.station.search.application.result.FindStationsItem;
import com.petrolprice.station_search_api.station.search.application.result.FindStationsResult;
import com.petrolprice.station_search_api.station.search.application.searcharea.RadiusSearchArea;
import com.petrolprice.station_search_api.station.ingestion.application.ProcessStationSnapshotService;
import com.petrolprice.station_search_api.station.domain.type.ProductType;
import com.petrolprice.station_search_api.integration.support.StationImportProbe;
import com.petrolprice.station_search_api.integration.support.StationSnapshotFixture;
import com.petrolprice.station_search_api.station.infrastructure.postgres.PostgresStationSearchPersistenceAdapter;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PostgresStationSearchPersistenceAdapterIT extends IntegrationTestBase {
    @Autowired
    private PostgresStationSearchPersistenceAdapter adapter;

    @Autowired
    private ProcessStationSnapshotService snapshotService;

    @Autowired
    private StationImportProbe probe;

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
                .containsExactly(
                        cheapestDiesel.externalId(), middleDiesel.externalId(), expensiveDiesel.externalId());
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

    private StationSnapshotFixture nearby(String offset) {
        BigDecimal delta = new BigDecimal(offset);
        return aStationSnapshot().withLocation(latitude.add(delta), longitude.add(delta));
    }

    private FindStationsQuery query(
            ProductType productType, FindStationsSort sort, int size) {
        return FindStationsQuery.builder()
                .searchArea(new RadiusSearchArea(latitude, longitude, 500))
                .productType(productType)
                .sortBy(sort)
                .pageRequest(FindStationsPageRequest.builder().size(size).build())
                .build();
    }

    private List<String> externalIds(FindStationsResult result) {
        return result.items().stream().map(item -> item.station().getExternalId()).toList();
    }
}
