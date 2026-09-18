package com.petrolprice.station_search_api.station.infrastructure.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class PostgresStationImportRepositoryAdapterTest {

    @Mock
    NamedParameterJdbcTemplate jdbcTemplate;

    @InjectMocks
    PostgresStationImportRepositoryAdapter adapter;

    @Test
    void shouldFailWhenIncrementProcessedStationsDoesNotUpdateOneRow() {
        UUID snapshotId = UUID.randomUUID();

        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(0);

        assertThatThrownBy(() -> adapter.incrementProcessedStations(snapshotId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(snapshotId.toString())
                .hasMessageContaining("updated 0 rows");
    }

    @Test
    void shouldFailWhenIncrementProcessedStationsUpdatesMoreThanOneRow() {
        UUID snapshotId = UUID.randomUUID();

        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(2);

        assertThatThrownBy(() -> adapter.incrementProcessedStations(snapshotId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("updated 2 rows");
    }

    @Test
    void shouldFailWhenMarkCompletedDoesNotUpdateOneRow() {
        UUID snapshotId = UUID.randomUUID();

        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(0);

        assertThatThrownBy(() -> adapter.markCompleted(snapshotId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(snapshotId.toString())
                .hasMessageContaining("updated 0 rows");
    }

    @Test
    void shouldReturnTrueWhenEventIsClaimed() {
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        boolean result = adapter.claimEvent(UUID.randomUUID(), UUID.randomUUID());

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEventIsAlreadyClaimed() {
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(0);

        boolean result = adapter.claimEvent(UUID.randomUUID(), UUID.randomUUID());

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnTrueWhenSnapshotIsClaimedForStatistics() {
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        boolean result = adapter.claimForStatisticsIfReady(UUID.randomUUID());

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenSnapshotIsNotReadyForStatistics() {
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(0);

        boolean result = adapter.claimForStatisticsIfReady(UUID.randomUUID());

        assertThat(result).isFalse();
    }
}
