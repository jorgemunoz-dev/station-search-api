package com.petrolprice.station_search_api.statistics.infrastructure.postgres;

import com.petrolprice.station_search_api.statistics.application.port.out.CurrentPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.port.out.HistoricalPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.port.out.StatisticsCalculationRepository;
import com.petrolprice.station_search_api.statistics.application.query.CurrentStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.GeographicScope;
import com.petrolprice.station_search_api.statistics.application.query.HistoricalStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.result.CurrentPriceStatistics;
import com.petrolprice.station_search_api.statistics.application.result.HistoricalPricePoint;
import com.petrolprice.station_search_api.statistics.application.result.RankedLocalityStatistics;
import com.petrolprice.station_search_api.statistics.application.result.StationPricePoint;
import com.petrolprice.station_search_api.statistics.domain.ProductType;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
        MapSqlParameterSource parameters = scopeParameters(
                baseParameters(query.countryCode(), query.productType()), query.scope());
        String sql =
                """
                WITH selected AS (
                    SELECT * FROM fuel_price_statistics
                    WHERE country = :country AND product_type = :productType
                      AND normalized_locality_name IS NOT DISTINCT FROM :locality
                      AND ((CAST(:adminArea1 AS varchar) IS NULL AND admin_area_1_name IS NULL)
                           OR LOWER(admin_area_1_name) = LOWER(CAST(:adminArea1 AS varchar)))
                      AND ((CAST(:adminArea2 AS varchar) IS NULL AND admin_area_2_name IS NULL)
                           OR LOWER(admin_area_2_name) = LOWER(CAST(:adminArea2 AS varchar)))
                      AND ((CAST(:adminArea3 AS varchar) IS NULL AND admin_area_3_name IS NULL)
                           OR LOWER(admin_area_3_name) = LOWER(CAST(:adminArea3 AS varchar)))
                    ORDER BY calculated_at DESC LIMIT 1
                ), country_statistics AS (
                    SELECT average_price FROM fuel_price_statistics
                    WHERE snapshot_id = (SELECT snapshot_id FROM selected)
                      AND country = :country AND product_type = :productType
                      AND normalized_locality_name IS NULL
                      AND admin_area_1_name IS NULL AND admin_area_2_name IS NULL AND admin_area_3_name IS NULL
                ), locality AS (
                    SELECT locality_name, normalized_locality_name,
                           admin_area_1_name, admin_area_2_name, admin_area_3_name
                    FROM search_location
                    WHERE country_code = :country
                      AND normalized_locality_name = (SELECT normalized_locality_name FROM selected)
                    ORDER BY accuracy DESC NULLS LAST LIMIT 1
                ), admin_area_2_statistics AS (
                    SELECT SUM(f.average_price * f.station_count) / NULLIF(SUM(f.station_count), 0) average_price
                    FROM fuel_price_statistics f
                    JOIN (
                        SELECT DISTINCT normalized_locality_name, admin_area_2_name
                        FROM search_location WHERE country_code = :country
                    ) metadata USING (normalized_locality_name)
                    WHERE f.snapshot_id = (SELECT snapshot_id FROM selected)
                      AND f.country = :country AND f.product_type = :productType
                      AND LOWER(metadata.admin_area_2_name) = LOWER((SELECT admin_area_2_name FROM locality))
                )
                SELECT selected.*, country_statistics.average_price country_average,
                       admin_area_2_statistics.average_price admin_area_2_average,
                       locality.locality_name,
                       locality.admin_area_1_name locality_admin_area_1_name,
                       locality.admin_area_2_name locality_admin_area_2_name,
                       locality.admin_area_3_name locality_admin_area_3_name,
                       selected.cheapest_station_id cheap_id, selected.minimum_price cheap_price,
                       cheap.external_id cheap_external_id, cheap.brand cheap_brand,
                       ST_Y(cheap.location::geometry) cheap_latitude,
                       ST_X(cheap.location::geometry) cheap_longitude,
                       selected.most_expensive_station_id expensive_id,
                       selected.maximum_price expensive_price,
                       expensive.external_id expensive_external_id, expensive.brand expensive_brand,
                       ST_Y(expensive.location::geometry) expensive_latitude,
                       ST_X(expensive.location::geometry) expensive_longitude
                FROM selected CROSS JOIN country_statistics LEFT JOIN locality ON TRUE
                LEFT JOIN admin_area_2_statistics ON TRUE
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
                    resolvedScope(rs),
                    average,
                    rs.getBigDecimal("minimum_price"),
                    rs.getBigDecimal("maximum_price"),
                    rs.getLong("station_count"),
                    station(rs, "cheap", null),
                    station(rs, "expensive", null),
                    difference(average, rs.getBigDecimal("country_average")),
                    difference(average, rs.getBigDecimal("admin_area_2_average")),
                    rs.getTimestamp("calculated_at").toInstant()));
        });
    }

    @Override
    public List<HistoricalPricePoint> history(HistoricalStatisticsQuery query) {
        MapSqlParameterSource parameters = scopeParameters(
                        baseParameters(query.countryCode(), query.productType()), query.scope())
                .addValue("historyFrom", query.from().minusDays(30))
                .addValue("from", query.from())
                .addValue("to", query.to());
        String sql =
                """
                WITH daily AS (
                    SELECT DISTINCT ON (calculated_at::date)
                           calculated_at::date observed_date, average_price, minimum_price,
                           maximum_price, station_count
                    FROM fuel_price_statistics
                    WHERE country = :country AND product_type = :productType
                      AND normalized_locality_name IS NOT DISTINCT FROM :locality
                      AND ((CAST(:adminArea1 AS varchar) IS NULL AND admin_area_1_name IS NULL)
                           OR LOWER(admin_area_1_name) = LOWER(CAST(:adminArea1 AS varchar)))
                      AND ((CAST(:adminArea2 AS varchar) IS NULL AND admin_area_2_name IS NULL)
                           OR LOWER(admin_area_2_name) = LOWER(CAST(:adminArea2 AS varchar)))
                      AND ((CAST(:adminArea3 AS varchar) IS NULL AND admin_area_3_name IS NULL)
                           OR LOWER(admin_area_3_name) = LOWER(CAST(:adminArea3 AS varchar)))
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
    public List<RankedLocalityStatistics> localities(
            String countryCode,
            ProductType productType,
            String adminArea1,
            String adminArea2,
            String adminArea3) {
        MapSqlParameterSource parameters = baseParameters(countryCode.toUpperCase(), productType)
                .addValue("adminArea1", blankToNull(adminArea1), Types.VARCHAR)
                .addValue("adminArea2", blankToNull(adminArea2), Types.VARCHAR)
                .addValue("adminArea3", blankToNull(adminArea3), Types.VARCHAR);
        String sql =
                """
                WITH latest_snapshot AS (
                    SELECT snapshot_id, average_price country_average
                    FROM fuel_price_statistics
                    WHERE country = :country AND product_type = :productType
                      AND normalized_locality_name IS NULL
                      AND admin_area_1_name IS NULL AND admin_area_2_name IS NULL AND admin_area_3_name IS NULL
                    ORDER BY calculated_at DESC LIMIT 1
                ), locality_metadata AS (
                    SELECT DISTINCT ON (normalized_locality_name)
                           normalized_locality_name, locality_name,
                           admin_area_1_name, admin_area_2_name, admin_area_3_name
                    FROM search_location
                    WHERE country_code = :country
                    ORDER BY normalized_locality_name, accuracy DESC NULLS LAST
                ), ranked AS (
                    SELECT f.*, l.locality_name,
                           l.admin_area_1_name locality_admin_area_1_name,
                           l.admin_area_2_name locality_admin_area_2_name,
                           l.admin_area_3_name locality_admin_area_3_name,
                           RANK() OVER (ORDER BY average_price, l.locality_name) cheapest_rank,
                           RANK() OVER (ORDER BY average_price DESC, l.locality_name) expensive_rank
                    FROM fuel_price_statistics f
                    JOIN locality_metadata l USING (normalized_locality_name)
                    WHERE f.snapshot_id = (SELECT snapshot_id FROM latest_snapshot)
                      AND f.country = :country AND f.product_type = :productType
                      AND (CAST(:adminArea1 AS varchar) IS NULL
                           OR LOWER(l.admin_area_1_name) = LOWER(CAST(:adminArea1 AS varchar)))
                      AND (CAST(:adminArea2 AS varchar) IS NULL
                           OR LOWER(l.admin_area_2_name) = LOWER(CAST(:adminArea2 AS varchar)))
                      AND (CAST(:adminArea3 AS varchar) IS NULL
                           OR LOWER(l.admin_area_3_name) = LOWER(CAST(:adminArea3 AS varchar)))
                )
                SELECT ranked.*, latest_snapshot.country_average,
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
            return new RankedLocalityStatistics(
                    rs.getString("normalized_locality_name"),
                    rs.getString("locality_name"),
                    rs.getString("locality_admin_area_1_name"),
                    rs.getString("locality_admin_area_2_name"),
                    rs.getString("locality_admin_area_3_name"),
                    average,
                    rs.getBigDecimal("minimum_price"),
                    rs.getBigDecimal("maximum_price"),
                    rs.getLong("station_count"),
                    station(rs, "cheap", null),
                    difference(average, rs.getBigDecimal("country_average")),
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
                  AND f.product_type = hp.product_type AND f.normalized_locality_name IS NULL
                  AND f.admin_area_1_name IS NULL AND f.admin_area_2_name IS NULL
                  AND f.admin_area_3_name IS NULL
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
                           location.normalized_locality_name, location.admin_area_1_name,
                           location.admin_area_2_name, location.admin_area_3_name
                    FROM historical_product_price hp
                    JOIN station s ON s.id = hp.station_id
                    LEFT JOIN LATERAL (
                        SELECT sl.normalized_locality_name, sl.admin_area_1_name,
                               sl.admin_area_2_name, sl.admin_area_3_name
                        FROM search_location sl
                        WHERE sl.country_code = s.country
                          AND sl.normalized_postal_code = UPPER(REGEXP_REPLACE(s.postal_code, '[^A-Za-z0-9]', '', 'g'))
                        ORDER BY similarity(
                            sl.normalized_locality_name,
                            LOWER(COALESCE(s.municipality, s.locality, ''))
                        ) DESC, sl.accuracy DESC NULLS LAST
                        LIMIT 1
                    ) location ON TRUE
                    WHERE hp.snapshot_id = :snapshotId AND hp.price > 0
                ), scopes AS (
                    SELECT snapshot_id, country, product_type, NULL::varchar normalized_locality_name,
                           NULL::varchar admin_area_1_name, NULL::varchar admin_area_2_name,
                           NULL::varchar admin_area_3_name, station_id, price
                    FROM source
                    UNION ALL
                    SELECT snapshot_id, country, product_type, normalized_locality_name,
                           NULL, NULL, NULL, station_id, price
                    FROM source WHERE normalized_locality_name IS NOT NULL
                    UNION ALL
                    SELECT snapshot_id, country, product_type, NULL, admin_area_1_name, NULL, NULL,
                           station_id, price FROM source WHERE admin_area_1_name IS NOT NULL
                    UNION ALL
                    SELECT snapshot_id, country, product_type, NULL, NULL, admin_area_2_name, NULL,
                           station_id, price FROM source WHERE admin_area_2_name IS NOT NULL
                    UNION ALL
                    SELECT snapshot_id, country, product_type, NULL, NULL, NULL, admin_area_3_name,
                           station_id, price FROM source WHERE admin_area_3_name IS NOT NULL
                )
                INSERT INTO fuel_price_statistics (
                    id, snapshot_id, country, product_type, normalized_locality_name,
                    admin_area_1_name, admin_area_2_name, admin_area_3_name,
                    average_price, minimum_price, maximum_price, station_count,
                    cheapest_station_id, most_expensive_station_id, calculated_at
                )
                SELECT gen_random_uuid(), snapshot_id, country, product_type, normalized_locality_name,
                       admin_area_1_name, admin_area_2_name, admin_area_3_name,
                       AVG(price), MIN(price), MAX(price), COUNT(*),
                       (ARRAY_AGG(station_id ORDER BY price, station_id))[1],
                       (ARRAY_AGG(station_id ORDER BY price DESC, station_id))[1], NOW()
                FROM scopes
                GROUP BY snapshot_id, country, product_type, normalized_locality_name,
                         admin_area_1_name, admin_area_2_name, admin_area_3_name
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource("snapshotId", snapshotId));
    }

    private MapSqlParameterSource baseParameters(String countryCode, ProductType productType) {
        return new MapSqlParameterSource()
                .addValue("country", countryCode.toUpperCase())
                .addValue("productType", productType.name());
    }

    private MapSqlParameterSource scopeParameters(MapSqlParameterSource parameters, GeographicScope scope) {
        return parameters
                .addValue("locality", scope.normalizedLocalityName(), Types.VARCHAR)
                .addValue(
                        "adminArea1",
                        scope.normalizedLocalityName() == null ? scope.adminArea1Name() : null,
                        Types.VARCHAR)
                .addValue(
                        "adminArea2",
                        scope.normalizedLocalityName() == null ? scope.adminArea2Name() : null,
                        Types.VARCHAR)
                .addValue(
                        "adminArea3",
                        scope.normalizedLocalityName() == null ? scope.adminArea3Name() : null,
                        Types.VARCHAR);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private GeographicScope resolvedScope(ResultSet rs) throws SQLException {
        String normalizedName = rs.getString("normalized_locality_name");
        if (normalizedName != null) {
            return GeographicScope.resolvedLocality(
                        normalizedName,
                        rs.getString("locality_name"),
                        rs.getString("locality_admin_area_1_name"),
                        rs.getString("locality_admin_area_2_name"),
                        rs.getString("locality_admin_area_3_name"));
        }
        return new GeographicScope(
                null,
                null,
                rs.getString("admin_area_1_name"),
                rs.getString("admin_area_2_name"),
                rs.getString("admin_area_3_name"));
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
