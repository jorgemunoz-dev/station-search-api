package com.petrolprice.station_search_api.integration;

import static com.petrolprice.station_search_api.domain.type.ProductType.DIESEL_A;
import static com.petrolprice.station_search_api.domain.type.ProductType.GASOLINE_95_E5;
import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.aStationSnapshot;
import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.price;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petrolprice.station_search_api.integration.support.StationImportProbe;
import com.petrolprice.station_search_api.integration.support.StationSnapshotFixture;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Full acceptance path: Rabbit batch -> import workflow -> PostgreSQL -> station HTTP search. */
@AutoConfigureMockMvc
class StationBatchSearchE2EIT extends IntegrationTestBase {
    private static final String EXCHANGE = "energy.snapshot.events";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StationImportProbe probe;

    @BeforeEach
    void cleanDatabase() {
        probe.clean();
    }

    @Test
    void shouldImportACompleteBatchAndExposeOnlyStationsMatchingTheSearch() throws Exception {
        UUID batchId = UUID.randomUUID();
        List<StationSnapshotFixture> batch = List.of(
                aStationSnapshot()
                        .withSnapshotId(batchId)
                        .withBrand("CHEAP_DIESEL")
                        .withLocation("40.4168", "-3.7038")
                        .withPrices(price(DIESEL_A, "1.399"), price(GASOLINE_95_E5, "1.599")),
                aStationSnapshot()
                        .withSnapshotId(batchId)
                        .withBrand("EXPENSIVE_DIESEL")
                        .withLocation("40.4175", "-3.7045")
                        .withPrices(price(DIESEL_A, "1.699")),
                aStationSnapshot()
                        .withSnapshotId(batchId)
                        .withBrand("GASOLINE_ONLY")
                        .withLocation("40.4180", "-3.7050")
                        .withPrices(price(GASOLINE_95_E5, "1.499")));

        batch.forEach(snapshot -> send("energy.snapshot.fuel.es.created", snapshot.message()));
        send("energy.snapshot.completed", batch.getFirst().completionEvent(batch.size()));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            StationImportProbe.ImportState state = probe.importState(batchId);
            assertThat(state.status()).isEqualTo("COMPLETED");
            assertThat(state.processedStations()).isEqualTo(batch.size());
            assertThat(state.publishedStations()).isEqualTo(batch.size());
            assertThat(probe.claimedEvents(batchId)).isEqualTo(batch.size());
            assertThat(batch).allSatisfy(snapshot -> assertThat(probe.stations(snapshot.externalId())).isOne());
        });

        mockMvc.perform(get("/stations")
                        .queryParam("searchMode", "RADIUS")
                        .queryParam("lat", "40.4168")
                        .queryParam("lng", "-3.7038")
                        .queryParam("radiusMeters", "1000")
                        .queryParam("productType", "DIESEL_A")
                        .queryParam("sortBy", "PRICE")
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stations", hasSize(2)))
                .andExpect(jsonPath("$.stations[0].brand", is("CHEAP_DIESEL")))
                .andExpect(jsonPath("$.stations[0].productPrices", hasSize(2)))
                .andExpect(jsonPath("$.stations[1].brand", is("EXPENSIVE_DIESEL")))
                .andExpect(jsonPath("$.page.numberOfElements", is(2)))
                .andExpect(jsonPath("$.page.hasNext", is(false)));
    }

    private void send(String routingKey, Object event) {
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
    }
}
