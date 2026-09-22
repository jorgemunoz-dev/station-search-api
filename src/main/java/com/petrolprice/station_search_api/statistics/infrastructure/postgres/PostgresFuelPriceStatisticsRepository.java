package com.petrolprice.station_search_api.statistics.infrastructure.postgres;

import com.petrolprice.station_search_api.statistics.application.port.out.CurrentPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.port.out.HistoricalPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.port.out.StatisticsCalculationRepository;
import com.petrolprice.station_search_api.statistics.application.query.CurrentStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.GeographicScope;
import com.petrolprice.station_search_api.statistics.application.query.HistoricalStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.result.CurrentPriceStatistics;
import com.petrolprice.station_search_api.statistics.application.result.HistoricalPricePoint;
import com.petrolprice.station_search_api.statistics.application.result.RankedAreaStatistics;
import com.petrolprice.station_search_api.statistics.application.result.StationPricePoint;
import com.petrolprice.station_search_api.statistics.domain.ProductType;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresFuelPriceStatisticsRepository
        implements CurrentPriceStatisticsRepository,
                HistoricalPriceStatisticsRepository,
                StatisticsCalculationRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<CurrentPriceStatistics> current(CurrentStatisticsQuery query) {
        MapSqlParameterSource parameters = baseParameters(query.countryCode(), query.productType());
        parameters.addValue("level", query.scope().level().name());
        parameters.addValue("scopeKey", scopeKey(query.scope()));

        String sql =
                """
            WITH selected AS (
                SELECT * FROM fuel_price_statistics
                WHERE country = :country AND product_type = :productType
                  AND geographic_level = :level AND scope_key = :scopeKey
                ORDER BY calculated_at DESC LIMIT 1
            ), national AS (
                SELECT average_price FROM fuel_price_statistics f
                WHERE f.snapshot_id = (SELECT snapshot_id FROM selected)
                  AND f.country = :country AND f.product_type = :productType
                  AND f.geographic_level = 'NATIONAL'
            ), provincial AS (
                SELECT average_price FROM fuel_price_statistics f
                WHERE f.snapshot_id = (SELECT snapshot_id FROM selected)
                  AND f.country = :country AND f.product_type = :productType
                  AND f.geographic_level = 'PROVINCE'
                  AND LOWER(f.area_name) = LOWER((SELECT province FROM selected))
            )
            SELECT selected.*, national.average_price national_average,
                   provincial.average_price provincial_average,
                   selected.cheapest_station_id cheap_id, selected.minimum_price cheap_price,
                   cheap.external_id cheap_external_id, cheap.brand cheap_brand,
                   ST_Y(cheap.location::geometry) cheap_latitude,
                   ST_X(cheap.location::geometry) cheap_longitude,
                   selected.most_expensive_station_id expensive_id,
                   selected.maximum_price expensive_price,
                   expensive.external_id expensive_external_id, expensive.brand expensive_brand,
                   ST_Y(expensive.location::geometry) expensive_latitude,
                   ST_X(expensive.location::geometry) expensive_longitude
            FROM selected CROSS JOIN national LEFT JOIN provincial ON TRUE
            JOIN station cheap ON cheap.id = selected.cheapest_station_id
            JOIN station expensive ON expensive.id = selected.most_expensive_station_id
            """;

        return jdbcTemplate.query(sql, parameters, rs -> {
            if (!rs.next() || rs.getLong("station_count") == 0) {
                return Optional.empty();
            }
            BigDecimal average = rs.getBigDecimal("average_price");
            return Optional.of(new CurrentPriceStatistics(
                    query.countryCode(),
                    query.productType(),
                    query.scope(),
                    average,
                    rs.getBigDecimal("minimum_price"),
                    rs.getBigDecimal("maximum_price"),
                    rs.getLong("station_count"),
                    station(rs, "cheap", null),
                    station(rs, "expensive", null),
                    difference(average, rs.getBigDecimal("national_average")),
                    difference(average, rs.getBigDecimal("provincial_average")),
                    rs.getTimestamp("calculated_at").toInstant()));
        });
    }

    @Override
    public List<HistoricalPricePoint> history(HistoricalStatisticsQuery query) {
        MapSqlParameterSource parameters = baseParameters(query.countryCode(), query.productType())
                .addValue("historyFrom", query.from().minusDays(30))
                .addValue("from", query.from())
                .addValue("to", query.to())
                .addValue("level", query.scope().level().name())
                .addValue("scopeKey", scopeKey(query.scope()));
        String sql =
                """
            WITH daily AS (
                SELECT DISTINCT ON (calculated_at::date)
                       calculated_at::date observed_date, average_price, minimum_price,
                       maximum_price, station_count
                FROM fuel_price_statistics
                WHERE country = :country AND product_type = :productType
                  AND geographic_level = :level AND scope_key = :scopeKey
                  AND calculated_at >= CAST(:historyFrom AS date)
                  AND calculated_at < (CAST(:to AS date) + INTERVAL '1 day')
                ORDER BY calculated_at::date, calculated_at DESC
            )
            SELECT d.*,
                   AVG(d.average_price) OVER () period_average_price,
                   MIN(d.minimum_price) OVER () period_minimum_price,
                   MAX(d.maximum_price) OVER () period_maximum_price,
                   d.average_price - d1.average_price change_1d,
                   d.average_price - d7.average_price change_7d,
                   d.average_price - d30.average_price change_30d,
                   100 * (d.average_price - d1.average_price) / NULLIF(d1.average_price, 0) percentage_1d,
                   100 * (d.average_price - d7.average_price) / NULLIF(d7.average_price, 0) percentage_7d,
                   100 * (d.average_price - d30.average_price) / NULLIF(d30.average_price, 0) percentage_30d
            FROM daily d
            LEFT JOIN daily d1 ON d1.observed_date = d.observed_date - 1
            LEFT JOIN daily d7 ON d7.observed_date = d.observed_date - 7
            LEFT JOIN daily d30 ON d30.observed_date = d.observed_date - 30
            WHERE d.observed_date BETWEEN CAST(:from AS date) AND CAST(:to AS date)
            ORDER BY d.observed_date
            """;
        return jdbcTemplate.query(
                sql,
                parameters,
                (rs, rowNum) -> new HistoricalPricePoint(
                        rs.getDate("observed_date").toLocalDate(),
                        rs.getBigDecimal("average_price"),
                        rs.getBigDecimal("minimum_price"),
                        rs.getBigDecimal("maximum_price"),
                        rs.getLong("station_count"),
                        rs.getBigDecimal("period_average_price"),
                        rs.getBigDecimal("period_minimum_price"),
                        rs.getBigDecimal("period_maximum_price"),
                        rs.getBigDecimal("change_1d"),
                        rs.getBigDecimal("change_7d"),
                        rs.getBigDecimal("change_30d"),
                        rs.getBigDecimal("percentage_1d"),
                        rs.getBigDecimal("percentage_7d"),
                        rs.getBigDecimal("percentage_30d")));
    }

    @Override
    public List<RankedAreaStatistics> provinces(String countryCode, ProductType productType) {
        MapSqlParameterSource parameters = baseParameters(countryCode.toUpperCase(), productType);
        String sql =
                """
            WITH latest_snapshot AS (
                SELECT snapshot_id, average_price national_average
                FROM fuel_price_statistics
                WHERE country = :country AND product_type = :productType
                  AND geographic_level = 'NATIONAL'
                ORDER BY calculated_at DESC LIMIT 1
            ), ranked AS (
                SELECT f.*,
                       RANK() OVER (ORDER BY average_price, province) cheapest_rank,
                       RANK() OVER (ORDER BY average_price DESC, province) expensive_rank
                FROM fuel_price_statistics f
                WHERE f.snapshot_id = (SELECT snapshot_id FROM latest_snapshot)
                  AND f.product_type = :productType AND f.geographic_level = 'PROVINCE'
            )
            SELECT ranked.*, latest_snapshot.national_average,
                   cheap.id cheap_id, cheap.external_id cheap_external_id,
                   cheap.brand cheap_brand, ranked.minimum_price cheap_price,
                   ST_Y(cheap.location::geometry) cheap_latitude,
                   ST_X(cheap.location::geometry) cheap_longitude
            FROM ranked CROSS JOIN latest_snapshot
            JOIN station cheap ON cheap.id = ranked.cheapest_station_id
            ORDER BY cheapest_rank
            """;
        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> {
            BigDecimal average = rs.getBigDecimal("average_price");
            return new RankedAreaStatistics(
                    rs.getString("area_name"),
                    average,
                    rs.getBigDecimal("minimum_price"),
                    rs.getBigDecimal("maximum_price"),
                    rs.getLong("station_count"),
                    station(rs, "cheap", null),
                    difference(average, rs.getBigDecimal("national_average")),
                    rs.getInt("cheapest_rank"),
                    rs.getInt("expensive_rank"));
        });
    }

    @Override
    public Optional<BigDecimal> stationPrice(UUID stationId, ProductType productType) {
        String sql =
                """
            SELECT hp.price FROM historical_product_price hp
            JOIN fuel_price_statistics f ON f.snapshot_id = hp.snapshot_id
              AND f.product_type = hp.product_type AND f.geographic_level = 'NATIONAL'
            WHERE hp.station_id = :stationId AND hp.product_type = :productType AND hp.price > 0
            ORDER BY f.calculated_at DESC LIMIT 1
            """;
        List<BigDecimal> prices = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("stationId", stationId)
                        .addValue("productType", productType.name()),
                (rs, rowNum) -> rs.getBigDecimal("price"));
        return prices.stream().findFirst();
    }

    @Override
    public void replaceForSnapshot(UUID snapshotId) {
        jdbcTemplate.update(
                "DELETE FROM fuel_price_statistics WHERE snapshot_id = :snapshotId",
                new MapSqlParameterSource("snapshotId", snapshotId));

        String sql =
                """
            WITH source AS (
                SELECT hp.snapshot_id, s.country, hp.product_type, hp.station_id, hp.price,
                       s.province, s.municipality, s.locality
                FROM historical_product_price hp
                JOIN station s ON s.id = hp.station_id
                WHERE hp.snapshot_id = :snapshotId AND hp.price > 0
            ), scopes AS (
                SELECT snapshot_id, country, product_type, 'NATIONAL' geographic_level,
                       'NATIONAL' scope_key, NULL::varchar area_name, NULL::varchar province,
                       station_id, price FROM source
                UNION ALL
                SELECT snapshot_id, country, product_type, 'PROVINCE',
                       'PROVINCE:' || LOWER(province), province, province, station_id, price
                FROM source WHERE province IS NOT NULL AND BTRIM(province) <> ''
                UNION ALL
                SELECT snapshot_id, country, product_type, 'MUNICIPALITY',
                       'MUNICIPALITY:' || LOWER(province) || ':' || LOWER(municipality),
                       municipality, province, station_id, price
                FROM source
                WHERE province IS NOT NULL AND BTRIM(province) <> ''
                  AND municipality IS NOT NULL AND BTRIM(municipality) <> ''
                UNION ALL
                SELECT snapshot_id, country, product_type, 'LOCALITY',
                       'LOCALITY:' || LOWER(province) || ':' || LOWER(locality),
                       locality, province, station_id, price
                FROM source
                WHERE province IS NOT NULL AND BTRIM(province) <> ''
                  AND locality IS NOT NULL AND BTRIM(locality) <> ''
            )
            INSERT INTO fuel_price_statistics (
                id, snapshot_id, country, product_type, geographic_level, scope_key,
                area_name, province, average_price, minimum_price, maximum_price,
                station_count, cheapest_station_id, most_expensive_station_id, calculated_at
            )
            SELECT gen_random_uuid(), snapshot_id, country, product_type, geographic_level, scope_key,
                   MAX(area_name), MAX(province), AVG(price), MIN(price), MAX(price), COUNT(*),
                   (ARRAY_AGG(station_id ORDER BY price, station_id))[1],
                   (ARRAY_AGG(station_id ORDER BY price DESC, station_id))[1], NOW()
            FROM scopes
            GROUP BY snapshot_id, country, product_type, geographic_level, scope_key
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource("snapshotId", snapshotId));
    }

    private MapSqlParameterSource baseParameters(String countryCode, ProductType productType) {
        return new MapSqlParameterSource().addValue("country", countryCode).addValue("productType", productType.name());
    }

    private String scopeKey(GeographicScope scope) {
        return switch (scope.level()) {
            case NATIONAL -> "NATIONAL";
            case PROVINCE -> "PROVINCE:" + scope.name().toLowerCase(java.util.Locale.ROOT);
            case MUNICIPALITY -> "MUNICIPALITY:" + scope.province().toLowerCase(java.util.Locale.ROOT) + ":"
                    + scope.name().toLowerCase(java.util.Locale.ROOT);
            case LOCALITY -> "LOCALITY:" + scope.province().toLowerCase(java.util.Locale.ROOT) + ":"
                    + scope.name().toLowerCase(java.util.Locale.ROOT);
        };
    }

    private StationPricePoint station(ResultSet rs, String prefix, Double distance) throws SQLException {
        UUID id = rs.getObject(prefix + "_id", UUID.class);
        return id == null
                ? null
                : new StationPricePoint(
                        id,
                        rs.getString(prefix + "_external_id"),
                        rs.getString(prefix + "_brand"),
                        rs.getBigDecimal(prefix + "_price"),
                        rs.getBigDecimal(prefix + "_latitude"),
                        rs.getBigDecimal(prefix + "_longitude"),
                        distance);
    }

    private BigDecimal difference(BigDecimal value, BigDecimal reference) {
        return value == null || reference == null ? null : value.subtract(reference);
    }
}
