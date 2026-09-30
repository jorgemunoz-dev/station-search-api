package com.petrolprice.station_search_api.station.ingestion.application;

import com.petrolprice.station_search_api.station.domain.model.Station;
import com.petrolprice.station_search_api.station.ingestion.application.command.ProcessStationSnapshotCommand;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.CurrentFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.HistoricalPriceRepositoryPort;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationImportRepositoryPort;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationRepositoryPort;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessStationSnapshotService {

    private final StationRepositoryPort stationRepositoryPort;
    private final CurrentFuelPriceRepositoryPort currentFuelPriceRepositoryPort;
    private final HistoricalPriceRepositoryPort historicalPriceRepositoryPort;
    private final StationImportRepositoryPort stationImportRepositoryPort;
    private final StationImportFinalizer stationImportFinalizer;

    @Transactional
    public void consume(ProcessStationSnapshotCommand command) {
        consume(List.of(command));
    }

    /**
     * Processes one broker delivery batch in a single database transaction. Progress is updated once per import
     * instead of once per event, avoiding the per-snapshot row becoming a serialization point for concurrent
     * consumers.
     */
    @Transactional
    public void consume(List<ProcessStationSnapshotCommand> commands) {
        Map<UUID, Integer> processedBySnapshot = new LinkedHashMap<>();
        Set<UUID> initializedSnapshots = new LinkedHashSet<>();

        for (ProcessStationSnapshotCommand command : commands) {
            if (initializedSnapshots.add(command.snapshotId())) {
                stationImportRepositoryPort.ensureExists(
                        command.snapshotId(), command.station().getCountry().name());
            }

            boolean claimed = stationImportRepositoryPort.claimEvent(command.snapshotId(), command.eventId());

            if (!claimed) {
                continue;
            }

            Station persistedStation = stationRepositoryPort.upsertFromSnapshot(command.station());
            if (isObservedToday(command)) {
                currentFuelPriceRepositoryPort.replaceCurrentPrices(
                        persistedStation.getId(), command.station().getProductPrices());
            }
            historicalPriceRepositoryPort.insertSnapshot(
                    command.snapshotId(),
                    persistedStation.getId(),
                    command.observedAt(),
                    command.station().getProductPrices());
            processedBySnapshot.merge(command.snapshotId(), 1, Integer::sum);
        }

        processedBySnapshot.forEach((snapshotId, count) -> {
            stationImportRepositoryPort.incrementProcessedStations(snapshotId, count);
            stationImportFinalizer.tryFinalize(snapshotId);
        });
    }

    private boolean isObservedToday(ProcessStationSnapshotCommand command) {
        LocalDate observedDate = command.observedAt().atZone(ZoneOffset.UTC).toLocalDate();
        return observedDate.equals(LocalDate.now(ZoneOffset.UTC));
    }
}
