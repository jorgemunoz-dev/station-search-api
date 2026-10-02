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
                    SELECT * FROM current_fuel_price_statistics
                    WHERE country = :country AND product_type = :productType
                      AND normalized_locality_name IS NULL
                      AND ((CAST(:adminArea1 AS varchar) IS NULL AND admin_area_1_name IS NULL)
                           OR BTRIM(LOWER(REGEXP_REPLACE(unaccent(admin_area_1_name), '[^[:alnum:]]+', ' ', 'g'))) = :adminArea1)
                      AND ((CAST(:adminArea2 AS varchar) IS NULL AND admin_area_2_name IS NULL)
                           OR BTRIM(LOWER(REGEXP_REPLACE(unaccent(admin_area_2_name), '[^[:alnum:]]+', ' ', 'g'))) = :adminArea2)
                      AND ((CAST(:adminArea3 AS varchar) IS NULL AND admin_area_3_name IS NULL)
                           OR BTRIM(LOWER(REGEXP_REPLACE(unaccent(admin_area_3_name), '[^[:alnum:]]+', ' ', 'g'))) = :adminArea3)
                ), country_statistics AS (
                    SELECT average_price FROM current_fuel_price_statistics
                    WHERE snapshot_id = (SELECT snapshot_id FROM selected)
                      AND country = :country AND product_type = :productType
                      AND normalized_locality_name IS NULL
                      AND admin_area_1_name IS NULL AND admin_area_2_name IS NULL AND admin_area_3_name IS NULL
                ), admin_area_2_statistics AS (
                    SELECT average_price FROM current_fuel_price_statistics f
                    WHERE f.snapshot_id=(SELECT snapshot_id FROM selected) AND f.country=:country
                      AND f.product_type=:productType AND f.normalized_locality_name IS NULL
                      AND BTRIM(LOWER(REGEXP_REPLACE(unaccent(f.admin_area_1_name),'[^[:alnum:]]+',' ','g')))=:adminArea1
                      AND BTRIM(LOWER(REGEXP_REPLACE(unaccent(f.admin_area_2_name),'[^[:alnum:]]+',' ','g')))=:adminArea2
                      AND f.admin_area_3_name IS NULL
                )
                SELECT selected.*, country_statistics.average_price country_average,
                       admin_area_2_statistics.average_price admin_area_2_average,
                       selected.cheapest_station_id cheap_id, selected.minimum_price cheap_price,
                       cheap.external_id cheap_external_id, cheap.brand cheap_brand,
                       ST_Y(cheap.location::geometry) cheap_latitude,
                       ST_X(cheap.location::geometry) cheap_longitude,
                       selected.most_expensive_station_id expensive_id,
                       selected.maximum_price expensive_price,
                       expensive.external_id expensive_external_id, expensive.brand expensive_brand,
                       ST_Y(expensive.location::geometry) expensive_latitude,
                       ST_X(expensive.location::geometry) expensive_longitude
                FROM selected CROSS JOIN country_statistics
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
                      AND normalized_locality_name IS NULL
                      AND ((CAST(:adminArea1 AS varchar) IS NULL AND admin_area_1_name IS NULL)
                           OR BTRIM(LOWER(REGEXP_REPLACE(unaccent(admin_area_1_name), '[^[:alnum:]]+', ' ', 'g'))) = :adminArea1)
                      AND ((CAST(:adminArea2 AS varchar) IS NULL AND admin_area_2_name IS NULL)
                           OR BTRIM(LOWER(REGEXP_REPLACE(unaccent(admin_area_2_name), '[^[:alnum:]]+', ' ', 'g'))) = :adminArea2)
                      AND ((CAST(:adminArea3 AS varchar) IS NULL AND admin_area_3_name IS NULL)
                           OR BTRIM(LOWER(REGEXP_REPLACE(unaccent(admin_area_3_name), '[^[:alnum:]]+', ' ', 'g'))) = :adminArea3)
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
                    FROM current_fuel_price_statistics
                    WHERE country = :country AND product_type = :productType
                      AND normalized_locality_name IS NULL
                      AND admin_area_1_name IS NULL AND admin_area_2_name IS NULL AND admin_area_3_name IS NULL
                ), locality_metadata AS (
                    SELECT DISTINCT ON (normalized_locality_name, admin_area_1_name, admin_area_2_name, admin_area_3_name)
                           normalized_locality_name, locality_name,
                           admin_area_1_name, admin_area_2_name, admin_area_3_name
                    FROM search_location
                    WHERE country_code = :country
                    ORDER BY normalized_locality_name, admin_area_1_name, admin_area_2_name, admin_area_3_name, accuracy DESC NULLS LAST
                ), ranked AS (
                    SELECT f.*, l.locality_name,
                           l.admin_area_1_name locality_admin_area_1_name,
                           l.admin_area_2_name locality_admin_area_2_name,
                           l.admin_area_3_name locality_admin_area_3_name,
                           RANK() OVER (ORDER BY average_price, l.locality_name) cheapest_rank,
                           RANK() OVER (ORDER BY average_price DESC, l.locality_name) expensive_rank
                    FROM current_fuel_price_statistics f
                    JOIN locality_metadata l ON l.normalized_locality_name=f.normalized_locality_name
                     AND l.admin_area_1_name IS NOT DISTINCT FROM f.admin_area_1_name
                     AND l.admin_area_2_name IS NOT DISTINCT FROM f.admin_area_2_name
                     AND l.admin_area_3_name IS NOT DISTINCT FROM f.admin_area_3_name
                    WHERE f.snapshot_id = (SELECT snapshot_id FROM latest_snapshot)
                      AND f.country = :country AND f.product_type = :productType
                      AND (CAST(:adminArea1 AS varchar) IS NULL
                           OR BTRIM(LOWER(REGEXP_REPLACE(unaccent(l.admin_area_1_name), '[^[:alnum:]]+', ' ', 'g'))) = BTRIM(LOWER(REGEXP_REPLACE(unaccent(CAST(:adminArea1 AS varchar)), '[^[:alnum:]]+', ' ', 'g'))))
                      AND (CAST(:adminArea2 AS varchar) IS NULL
                           OR BTRIM(LOWER(REGEXP_REPLACE(unaccent(l.admin_area_2_name), '[^[:alnum:]]+', ' ', 'g'))) = BTRIM(LOWER(REGEXP_REPLACE(unaccent(CAST(:adminArea2 AS varchar)), '[^[:alnum:]]+', ' ', 'g'))))
                      AND (CAST(:adminArea3 AS varchar) IS NULL
                           OR BTRIM(LOWER(REGEXP_REPLACE(unaccent(l.admin_area_3_name), '[^[:alnum:]]+', ' ', 'g'))) = BTRIM(LOWER(REGEXP_REPLACE(unaccent(CAST(:adminArea3 AS varchar)), '[^[:alnum:]]+', ' ', 'g'))))
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
                JOIN current_fuel_price_statistics f ON f.snapshot_id = hp.snapshot_id
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
                           hp.observed_at,
                           COALESCE(s.normalized_locality_name, location.normalized_locality_name) normalized_locality_name,
                           COALESCE(s.admin_area_1_name, location.admin_area_1_name) admin_area_1_name,
                           COALESCE(s.admin_area_2_name, location.admin_area_2_name) admin_area_2_name,
                           COALESCE(s.admin_area_3_name, location.admin_area_3_name) admin_area_3_name
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
                            COALESCE(s.normalized_locality_name, '')
                        ) DESC, sl.accuracy DESC NULLS LAST
                        LIMIT 1
                    ) location ON TRUE
                    WHERE hp.snapshot_id = :snapshotId AND hp.price > 0
                ), scopes AS (
                    SELECT snapshot_id, country, product_type, NULL::varchar normalized_locality_name,
                           NULL::varchar admin_area_1_name, NULL::varchar admin_area_2_name,
                           NULL::varchar admin_area_3_name, station_id, price, observed_at
                    FROM source
                    UNION ALL
                    SELECT snapshot_id, country, product_type, normalized_locality_name,
                           admin_area_1_name, admin_area_2_name, admin_area_3_name, station_id, price, observed_at
                    FROM source WHERE normalized_locality_name IS NOT NULL
                    UNION ALL
                    SELECT snapshot_id, country, product_type, NULL, admin_area_1_name, NULL, NULL,
                           station_id, price, observed_at FROM source WHERE admin_area_1_name IS NOT NULL
                    UNION ALL
                    SELECT snapshot_id, country, product_type, NULL, admin_area_1_name, admin_area_2_name, NULL,
                           station_id, price, observed_at FROM source WHERE admin_area_1_name IS NOT NULL AND admin_area_2_name IS NOT NULL
                    UNION ALL
                    SELECT snapshot_id, country, product_type, NULL, admin_area_1_name, admin_area_2_name, admin_area_3_name,
                           station_id, price, observed_at FROM source WHERE admin_area_1_name IS NOT NULL AND admin_area_2_name IS NOT NULL AND admin_area_3_name IS NOT NULL
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
                       (ARRAY_AGG(station_id ORDER BY price DESC, station_id))[1], MAX(observed_at)
                FROM scopes
                GROUP BY snapshot_id, country, product_type, normalized_locality_name,
                         admin_area_1_name, admin_area_2_name, admin_area_3_name
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource("snapshotId", snapshotId));
        promoteToCurrent(snapshotId);
    }

    private void promoteToCurrent(UUID snapshotId) {
        String sql =
                """
                WITH incoming AS (
                    SELECT country, snapshot_id, MAX(calculated_at) calculated_at
                    FROM fuel_price_statistics
                    WHERE snapshot_id = :snapshotId
                    GROUP BY country, snapshot_id
                ), eligible AS (
                    SELECT incoming.*
                    FROM incoming
                    WHERE NOT EXISTS (
                        SELECT 1 FROM current_fuel_price_statistics published
                        WHERE published.country = incoming.country
                          AND published.calculated_at > incoming.calculated_at
                    )
                ), deleted AS (
                    DELETE FROM current_fuel_price_statistics published
                    USING eligible
                    WHERE published.country = eligible.country
                    RETURNING published.id
                )
                INSERT INTO current_fuel_price_statistics
                SELECT statistics.*
                FROM fuel_price_statistics statistics
                JOIN eligible USING (country, snapshot_id)
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
                .addValue("adminArea1", normalize(scope.adminArea1Name()), Types.VARCHAR)
                .addValue("adminArea2", normalize(scope.adminArea2Name()), Types.VARCHAR)
                .addValue("adminArea3", normalize(scope.adminArea3Name()), Types.VARCHAR);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private GeographicScope resolvedScope(ResultSet rs) throws SQLException {
        return new GeographicScope(rs.getString("admin_area_1_name"), rs.getString("admin_area_2_name"), rs.getString("admin_area_3_name"));
    }

    private String normalize(String value) {
        return com.petrolprice.station_search_api.station.search.application.searcharea.AdministrativeHierarchyNormalizer.normalize(value);
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
