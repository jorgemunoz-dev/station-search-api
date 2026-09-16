package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import com.petrolprice.station_search_api.application.port.out.StationImportRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PostgresStationImportRepositoryAdapter implements StationImportRepositoryPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void ensureExists(UUID snapshotId) {
        String sql = """
            INSERT INTO station_import (
                snapshot_id,
                status,
                processed_stations,
                publishing_completed,
                created_at
            )
            VALUES (
                :snapshotId,
                'PROCESSING',
                0,
                FALSE,
                NOW()
            )
            ON CONFLICT (snapshot_id) DO NOTHING
            """;

        jdbcTemplate.update(
            sql,
            new MapSqlParameterSource("snapshotId", snapshotId)
        );
    }

    @Override
    public boolean claimEvent(UUID snapshotId, UUID eventId) {
        String sql = """
            INSERT INTO station_import_event (
                snapshot_id,
                event_id,
                processed_at
            )
            VALUES (
                :snapshotId,
                :eventId,
                NOW()
            )
            ON CONFLICT (snapshot_id, event_id) DO NOTHING
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("snapshotId", snapshotId)
            .addValue("eventId", eventId);

        return jdbcTemplate.update(sql, params) == 1;
    }

    @Override
    public void incrementProcessedStations(UUID snapshotId) {
        String sql = """
            UPDATE station_import
            SET processed_stations = processed_stations + 1,
                updated_at = NOW()
            WHERE snapshot_id = :snapshotId
            """;

        int updated = jdbcTemplate.update(
            sql,
            new MapSqlParameterSource("snapshotId", snapshotId)
        );

        assertOneRowUpdated(updated, snapshotId);
    }

    @Override
    public void markPublishingCompleted(UUID snapshotId, int publishedStations, Instant completedAt) {
        String sql = """
            INSERT INTO station_import (
                snapshot_id,
                status,
                processed_stations,
                publishing_completed,
                published_stations,
                publishing_completed_at,
                created_at,
                updated_at
            )
            VALUES (
                :snapshotId,
                'PROCESSING',
                0,
                TRUE,
                :publishedStations,
                :completedAt,
                NOW(),
                NOW()
            )
            ON CONFLICT (snapshot_id)
            DO UPDATE SET
                publishing_completed = TRUE,
                published_stations = EXCLUDED.published_stations,
                publishing_completed_at = EXCLUDED.publishing_completed_at,
                updated_at = NOW()
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("snapshotId", snapshotId)
            .addValue("publishedStations", publishedStations)
            .addValue(
                "completedAt",
                completedAt.atOffset(ZoneOffset.UTC)
            );

        jdbcTemplate.update(sql, params);
    }

    @Override
    public boolean claimForStatisticsIfReady(UUID snapshotId) {
        String sql = """
            UPDATE station_import
            SET status = 'CALCULATING_STATISTICS',
                statistics_started_at = NOW(),
                updated_at = NOW()
            WHERE snapshot_id = :snapshotId
              AND status = 'PROCESSING'
              AND publishing_completed = TRUE
              AND published_stations IS NOT NULL
              AND processed_stations = published_stations
            """;

        int updated = jdbcTemplate.update(
            sql,
            new MapSqlParameterSource("snapshotId", snapshotId)
        );

        return updated == 1;
    }

    @Override
    public void markCompleted(UUID snapshotId) {
        String sql = """
            UPDATE station_import
            SET status = 'COMPLETED',
                completed_at = NOW(),
                updated_at = NOW()
            WHERE snapshot_id = :snapshotId
              AND status = 'CALCULATING_STATISTICS'
            """;

        int updated = jdbcTemplate.update(
            sql,
            new MapSqlParameterSource("snapshotId", snapshotId)
        );

        assertOneRowUpdated(updated, snapshotId);
    }

    private void assertOneRowUpdated(int updatedRows, UUID snapshotId) {
        if (updatedRows != 1) {
            throw new IllegalStateException(
                "Expected to update station import %s but updated %d rows"
                    .formatted(snapshotId, updatedRows)
            );
        }
    }
}
