package com.petrolprice.station_search_api.statistics.application.command;

import java.util.UUID;

public record CalculateFuelPriceStatisticsCommand(UUID snapshotId) {
    public CalculateFuelPriceStatisticsCommand {
        if (snapshotId == null) {
            throw new IllegalArgumentException("snapshotId is required");
        }
    }
}
