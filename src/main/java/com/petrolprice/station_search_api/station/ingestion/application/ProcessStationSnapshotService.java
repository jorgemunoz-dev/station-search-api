package com.petrolprice.station_search_api.station.ingestion.application;

import com.petrolprice.station_search_api.station.domain.model.Station;
import com.petrolprice.station_search_api.station.ingestion.application.command.ProcessStationSnapshotCommand;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.CurrentFuelPriceRepositoryPort;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.GeographicAreaResolverPort;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.HistoricalPriceRepositoryPort;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationImportRepositoryPort;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.StationRepositoryPort;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessStationSnapshotService {

    private final StationRepositoryPort stationRepositoryPort;
    private final CurrentFuelPriceRepositoryPort currentFuelPriceRepositoryPort;
    private final HistoricalPriceRepositoryPort historicalPriceRepositoryPort;
    private final StationImportRepositoryPort stationImportRepositoryPort;
    private final GeographicAreaResolverPort geographicAreaResolverPort;

    @Transactional
    public void consume(ProcessStationSnapshotCommand command) {

        stationImportRepositoryPort.ensureExists(
                command.snapshotId(), command.station().getCountry().name());

        boolean claimed = stationImportRepositoryPort.claimEvent(command.snapshotId(), command.eventId());

        if (!claimed) {
            return;
        }

        Station station = withCanonicalGeography(command.station());
        Station persistedStation = stationRepositoryPort.upsertFromSnapshot(station);
        if (isObservedToday(command)) {
            currentFuelPriceRepositoryPort.replaceCurrentPrices(
                    persistedStation.getId(), command.station().getProductPrices());
        }
        historicalPriceRepositoryPort.insertSnapshot(
                command.snapshotId(),
                persistedStation.getId(),
                command.observedAt(),
                command.station().getProductPrices());
    }

    private Station withCanonicalGeography(Station station) {
        if (station.getAddress() == null) {
            return station;
        }
        var resolution = geographicAreaResolverPort.resolve(station.getCountry(), station.getAddress());
        var address = station.getAddress().toBuilder()
                .adminArea1Id(resolution.adminArea1Id())
                .adminArea2Id(resolution.adminArea2Id())
                .adminArea3Id(resolution.adminArea3Id())
                .localityId(resolution.localityId())
                .build();
        return station.toBuilder().address(address).build();
    }

    private boolean isObservedToday(ProcessStationSnapshotCommand command) {
        LocalDate observedDate = command.observedAt().atZone(ZoneOffset.UTC).toLocalDate();
        return observedDate.equals(LocalDate.now(ZoneOffset.UTC));
    }
}
