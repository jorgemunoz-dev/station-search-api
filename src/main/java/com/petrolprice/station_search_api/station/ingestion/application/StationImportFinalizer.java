package com.petrolprice.station_search_api.station.ingestion.application;

import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationImportRepositoryPort;
import com.petrolprice.station_search_api.statistics.application.CalculateFuelPriceStatisticsUseCase;
import com.petrolprice.station_search_api.statistics.application.command.CalculateFuelPriceStatisticsCommand;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StationImportFinalizer {

    private final StationImportRepositoryPort stationImportRepositoryPort;
    private final CalculateFuelPriceStatisticsUseCase statisticsCalculator;
    private final EntityManager entityManager;
    private final MeterRegistry meterRegistry;

    @Transactional
    public void tryFinalize(UUID snapshotId) {
        boolean claimed = stationImportRepositoryPort.claimForStatisticsIfReady(snapshotId);

        if (!claimed) {
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        boolean completed = false;
        try {
            entityManager.flush();
            statisticsCalculator.calculate(new CalculateFuelPriceStatisticsCommand(snapshotId));
            stationImportRepositoryPort.markCompleted(snapshotId);
            completed = true;

            log.atInfo()
                    .addKeyValue("event", "station_import_completed")
                    .addKeyValue("snapshotId", snapshotId)
                    .log("Station import statistics completed");
        } finally {
            sample.stop(Timer.builder("station.import.statistics.duration")
                    .description("Time spent calculating and persisting snapshot statistics")
                    .tag("outcome", completed ? "SUCCESS" : "FAILURE")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }
}
