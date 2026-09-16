package com.petrolprice.station_search_api.integration;

import static com.petrolprice.station_search_api.integration.support.StationSnapshotFixture.aStationSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

import com.petrolprice.station_search_api.station.ingestion.application.CompleteStationPublishingService;
import com.petrolprice.station_search_api.station.ingestion.application.ProcessStationSnapshotService;
import com.petrolprice.station_search_api.integration.support.StationImportProbe;
import com.petrolprice.station_search_api.integration.support.StationSnapshotFixture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integrates both workflow services with the real PostgreSQL adapter. Tests describe business
 * scenarios; fixture creation and SQL knowledge live in reusable support classes.
 */
class StationImportWorkflowIT extends IntegrationTestBase {
    @Autowired
    private ProcessStationSnapshotService snapshotService;

    @Autowired
    private CompleteStationPublishingService completionService;

    @Autowired
    private StationImportProbe probe;

    @BeforeEach
    void cleanDatabase() {
        probe.clean();
    }

    @Test
    void shouldIgnoreARedeliveredEvent() {
        StationSnapshotFixture snapshot = aStationSnapshot();

        snapshotService.consume(snapshot.processCommand());
        snapshotService.consume(snapshot.processCommand());

        assertThat(probe.claimedEvents(snapshot.snapshotId())).isOne();
        assertThat(probe.stations(snapshot.externalId())).isOne();
        assertThat(probe.historicalPrices(snapshot.externalId())).isOne();
        assertThat(probe.importState(snapshot.snapshotId()).processedStations()).isOne();
    }

    @Test
    void shouldFinishWhenCompletionArrivesBeforeTheLastStation() {
        StationSnapshotFixture snapshot = aStationSnapshot();

        completionService.complete(snapshot.completionCommand(1));
        assertThat(probe.importState(snapshot.snapshotId()).status()).isEqualTo("PROCESSING");

        snapshotService.consume(snapshot.processCommand());

        assertCompleted(snapshot);
    }

    @Test
    void shouldFinishWhenCompletionArrivesAfterTheLastStation() {
        StationSnapshotFixture snapshot = aStationSnapshot();

        snapshotService.consume(snapshot.processCommand());
        completionService.complete(snapshot.completionCommand(1));

        assertCompleted(snapshot);
    }

    @Test
    void shouldClaimADuplicateEventOnlyOnceWhenDeliveriesRace() throws Exception {
        StationSnapshotFixture snapshot = aStationSnapshot();
        runConcurrently(
                () -> snapshotService.consume(snapshot.processCommand()),
                () -> snapshotService.consume(snapshot.processCommand()));

        assertThat(probe.claimedEvents(snapshot.snapshotId())).isOne();
        assertThat(probe.importState(snapshot.snapshotId()).processedStations()).isOne();
        assertThat(probe.historicalPrices(snapshot.externalId())).isOne();
    }

    @Test
    void shouldFinalizeExactlyOnceWhenLastStationAndCompletionRace() throws Exception {
        StationSnapshotFixture snapshot = aStationSnapshot();

        runConcurrently(
                () -> snapshotService.consume(snapshot.processCommand()),
                () -> completionService.complete(snapshot.completionCommand(1)));

        assertCompleted(snapshot);
        assertThat(probe.claimedEvents(snapshot.snapshotId())).isOne();
    }

    private void assertCompleted(StationSnapshotFixture snapshot) {
        StationImportProbe.ImportState state = probe.importState(snapshot.snapshotId());
        assertThat(state.status()).isEqualTo("COMPLETED");
        assertThat(state.processedStations()).isOne();
        assertThat(state.publishedStations()).isOne();
        assertThat(state.publishingCompleted()).isTrue();
    }

    private void runConcurrently(Runnable first, Runnable second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> firstResult = executor.submit(() -> runAfterBarrier(first, ready, start));
            Future<?> secondResult = executor.submit(() -> runAfterBarrier(second, ready, start));
            ready.await();
            start.countDown();
            firstResult.get();
            secondResult.get();
        }
    }

    private void runAfterBarrier(Runnable action, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await();
            action.run();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
