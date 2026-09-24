package com.petrolprice.station_search_api.statistics.application.port.out;

import java.util.UUID;

public interface StatisticsCalculationRepository {
    void replaceForSnapshot(UUID snapshotId);

    int backfillMissingScopes();
}
