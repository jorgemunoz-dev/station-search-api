package com.petrolprice.station_search_api.integration;

import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.aStationSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.petrolprice.station_search_api.integration.support.StationImportProbe;
import com.petrolprice.station_search_api.integration.support.StationSnapshotFixture;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/** Thin messaging contract test. Workflow permutations belong in {@link StationImportWorkflowIT}. */
class StationSnapshotRabbitIT extends IntegrationTestBase {
    private static final String EXCHANGE = "energy.snapshot.events";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StationImportProbe probe;

    @BeforeEach
    void cleanDatabase() {
        probe.clean();
    }

    @Test
    void shouldConsumeTheCurrentEnvelopeAndCompleteTheImport() {
        StationSnapshotFixture snapshot = aStationSnapshot().withBrand("CEPSA");

        send("energy.snapshot.fuel.es.created", snapshot.message());
        send("energy.snapshot.completed", snapshot.completionEvent(1));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(probe.stations(snapshot.externalId())).isOne();
            assertThat(probe.importState(snapshot.snapshotId()).status()).isEqualTo("COMPLETED");
        });
    }

    @Test
    void shouldIgnoreARedeliveredRabbitMessage() {
        StationSnapshotFixture snapshot = aStationSnapshot();

        send("energy.snapshot.fuel.es.created", snapshot.message());
        send("energy.snapshot.fuel.es.created", snapshot.message());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(probe.claimedEvents(snapshot.snapshotId())).isOne();
            assertThat(probe.historicalPrices(snapshot.externalId())).isOne();
        });
    }

    private void send(String routingKey, Object event) {
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
    }
}
