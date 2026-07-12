package com.petrolprice.station_search_api.integration;

import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsQuery;
import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsResult;
import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsSort;
import com.petrolprice.station_search_api.domain.type.Day;
import com.petrolprice.station_search_api.domain.type.ProductType;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.PostgresStationSearchPersistenceAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Sql(
    scripts = "/sql/station-search-dataset.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
public class PostgresStationSearchPersistenceAdapterIT extends IntegrationTestBase {

    @Autowired
    private PostgresStationSearchPersistenceAdapter adapter;

    @Test
    void shouldFilterStationsByRadius() {
        FindStationsQuery query = new FindStationsQuery(
            BigDecimal.valueOf(36.878694),
            BigDecimal.valueOf(-4.844639),
            500,
            null,
            FindStationsSort.DISTANCE,
            10
        );

        FindStationsResult result = adapter.search(query);

        assertThat(result.stations())
            .extracting(item -> item.station().getId())
            .containsExactlyInAnyOrder(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("66666666-6666-6666-6666-666666666666")
            );
    }

    @Test
    void shouldFilterStationsByProductType() {
        FindStationsQuery query = FindStationsQuery.builder()
            .latitude(BigDecimal.valueOf(36.878694))
            .longitude(BigDecimal.valueOf(-4.844639))
            .radiusMeters(500)
            .productType(ProductType.DIESEL_A)
            .sortBy(FindStationsSort.PRICE)
            .limit(10)
            .build();


        FindStationsResult result = adapter.search(query);

        assertThat(result.stations())
            .extracting(item -> item.station().getId())
            .containsExactlyInAnyOrder(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("66666666-6666-6666-6666-666666666666")
            );
    }

    @Test
    void shouldSortStationsByPrice() {
        FindStationsQuery query = FindStationsQuery.builder()
            .latitude(BigDecimal.valueOf(36.878694))
            .longitude(BigDecimal.valueOf(-4.844639))
            .radiusMeters(500)
            .productType(ProductType.DIESEL_A)
            .sortBy(FindStationsSort.PRICE)
            .limit(10)
            .build();

        FindStationsResult result = adapter.search(query);

        assertThat(result.stations())
            .extracting(item -> item.station().getId())
            .containsExactly(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                UUID.fromString("22222222-2222-2222-2222-222222222222")
            );
    }

    @Test
    void shouldRespectLimitAfterFilteringAndSorting() {
        FindStationsQuery query = FindStationsQuery.builder()
            .latitude(BigDecimal.valueOf(36.878694))
            .longitude(BigDecimal.valueOf(-4.844639))
            .radiusMeters(500)
            .productType(ProductType.DIESEL_A)
            .sortBy(FindStationsSort.PRICE)
            .limit(2)
            .build();

        FindStationsResult result = adapter.search(query);

        assertThat(result.stations())
            .extracting(item -> item.station().getId())
            .containsExactly(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("66666666-6666-6666-6666-666666666666")
            );
    }

    @Test
    void shouldLoadStationDetailsWithoutDuplicates() {
        FindStationsQuery query = FindStationsQuery.builder()
            .latitude(BigDecimal.valueOf(36.878694))
            .longitude(BigDecimal.valueOf(-4.844639))
            .radiusMeters(500)
            .productType(ProductType.DIESEL_A)
            .sortBy(FindStationsSort.PRICE)
            .limit(10)
            .build();

        FindStationsResult result = adapter.search(query);

        assertThat(result.stations())
            .extracting(item -> item.station().getId())
            .containsOnlyOnce(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        var s1 = result.stations()
            .stream()
            .filter(item -> item.station().getId().equals(UUID.fromString("11111111-1111-1111-1111-111111111111")))
            .findFirst()
            .orElseThrow();

        assertThat(s1.station().getProductPrices())
            .extracting(productPrice -> productPrice.getProductType())
            .containsExactlyInAnyOrder(
                ProductType.DIESEL_A,
                ProductType.GASOLINE_95_E5
            );

        assertThat(s1.station().getOpeningPeriods())
            .hasSize(2);

        assertThat(s1.station().getOpeningPeriods())
            .anySatisfy(period -> {
                assertThat(period.getDays())
                    .containsExactlyInAnyOrder(Day.MON, Day.TUE, Day.WED);

                assertThat(period.getOpen().toString()).isEqualTo("08:00");
                assertThat(period.getClose().toString()).isEqualTo("22:00");
            });

        assertThat(s1.station().getOpeningPeriods())
            .anySatisfy(period -> {
                assertThat(period.getDays())
                    .containsExactly(Day.SAT);

                assertThat(period.getOpen().toString()).isEqualTo("09:00");
                assertThat(period.getClose().toString()).isEqualTo("20:00");
            });
    }

}
