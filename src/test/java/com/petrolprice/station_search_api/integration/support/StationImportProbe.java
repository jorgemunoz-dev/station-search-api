package com.petrolprice.station_search_api.integration.support;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Read-only assertions facade which keeps integration tests independent from persistence details. */
@Component
@RequiredArgsConstructor
public class StationImportProbe {
    private final JdbcClient jdbcClient;

    public ImportState importState(UUID snapshotId) {
        return jdbcClient
                .sql("""
                    SELECT status, processed_stations, published_stations, publishing_completed
                    FROM station_import
                    WHERE snapshot_id = :snapshotId
                    """)
                .param("snapshotId", snapshotId)
                .query((rs, rowNum) -> new ImportState(
                        rs.getString("status"),
                        rs.getInt("processed_stations"),
                        (Integer) rs.getObject("published_stations"),
                        rs.getBoolean("publishing_completed")))
                .single();
    }

    public int claimedEvents(UUID snapshotId) {
        return count("SELECT COUNT(*) FROM station_import_event WHERE snapshot_id = :id", snapshotId);
    }

    public int stations(String externalId) {
        return count("SELECT COUNT(*) FROM station WHERE external_id = :id", externalId);
    }

    public int historicalPrices(String externalId) {
        return jdbcClient
                .sql("""
                    SELECT COUNT(*)
                    FROM historical_product_price price
                    JOIN station ON station.id = price.station_id
                    WHERE station.external_id = :externalId
                    """)
                .param("externalId", externalId)
                .query(Integer.class)
                .single();
    }

    public void clean() {
        jdbcClient.sql("""
                TRUNCATE TABLE station_import, historical_product_price,
                    station_current_product_price, station_opening_period, station
                CASCADE
                """).update();
    }

    private int count(String sql, Object id) {
        return jdbcClient.sql(sql).param("id", id).query(Integer.class).single();
    }

    public record ImportState(String status, int processedStations, Integer publishedStations, boolean publishingCompleted) {}
}
