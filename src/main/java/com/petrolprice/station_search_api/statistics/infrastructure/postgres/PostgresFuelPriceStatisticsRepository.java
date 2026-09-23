package com.petrolprice.station_search_api.statistics.infrastructure.postgres;

import com.petrolprice.station_search_api.statistics.application.port.out.CurrentPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.port.out.HistoricalPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.port.out.StatisticsCalculationRepository;
import com.petrolprice.station_search_api.statistics.application.query.CurrentStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.GeographicScope;
import com.petrolprice.station_search_api.statistics.application.query.HistoricalStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.result.AdministrativeArea;
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
        UUID areaId = resolveAreaId(query.countryCode(), query.scope());
        parameters.addValue("areaId", areaId);

        String sql =
                """
            WITH selected AS (
                SELECT * FROM fuel_price_statistics
                WHERE country = :country AND product_type = :productType
                  AND administrative_area_id IS NOT DISTINCT FROM CAST(:areaId AS uuid)
                ORDER BY calculated_at DESC LIMIT 1
            ), national AS (
                SELECT average_price FROM fuel_price_statistics f
                WHERE f.snapshot_id = (SELECT snapshot_id FROM selected)
                  AND f.country = :country AND f.product_type = :productType
                  AND f.administrative_area_id IS NULL
            ), parent_area AS (
                SELECT average_price FROM fuel_price_statistics f
                WHERE f.snapshot_id = (SELECT snapshot_id FROM selected)
                  AND f.country = :country AND f.product_type = :productType
                  AND f.administrative_area_id = (
                      SELECT parent_id FROM administrative_area
                      WHERE id = (SELECT administrative_area_id FROM selected)
                  )
            )
            SELECT selected.*, national.average_price national_average,
                   parent_area.average_price parent_average,
                   area.type area_type, area.name area_name, parent.name parent_name,
                   selected.cheapest_station_id cheap_id, selected.minimum_price cheap_price,
                   cheap.external_id cheap_external_id, cheap.brand cheap_brand,
                   ST_Y(cheap.location::geometry) cheap_latitude,
                   ST_X(cheap.location::geometry) cheap_longitude,
                   selected.most_expensive_station_id expensive_id,
                   selected.maximum_price expensive_price,
                   expensive.external_id expensive_external_id, expensive.brand expensive_brand,
                   ST_Y(expensive.location::geometry) expensive_latitude,
                   ST_X(expensive.location::geometry) expensive_longitude
            FROM selected CROSS JOIN national LEFT JOIN parent_area ON TRUE
            LEFT JOIN administrative_area area ON area.id = selected.administrative_area_id
            LEFT JOIN administrative_area parent ON parent.id = area.parent_id
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
                    difference(average, rs.getBigDecimal("national_average")),
                    difference(average, rs.getBigDecimal("parent_average")),
                    rs.getTimestamp("calculated_at").toInstant()));
        });
    }

    @Override
    public List<HistoricalPricePoint> history(HistoricalStatisticsQuery query) {
        MapSqlParameterSource parameters = baseParameters(query.countryCode(), query.productType())
                .addValue("historyFrom", query.from().minusDays(30))
                .addValue("from", query.from())
                .addValue("to", query.to())
                .addValue("areaId", resolveAreaId(query.countryCode(), query.scope()));
        String sql =
                """
            WITH daily AS (
                SELECT DISTINCT ON (calculated_at::date)
                       calculated_at::date observed_date, average_price, minimum_price,
                       maximum_price, station_count
                FROM fuel_price_statistics
                WHERE country = :country AND product_type = :productType
                  AND administrative_area_id IS NOT DISTINCT FROM CAST(:areaId AS uuid)
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
        return areas(countryCode, productType, null, "PROVINCE");
    }

    @Override
    public List<RankedAreaStatistics> areas(
            String countryCode, ProductType productType, UUID parentAreaId, String areaType) {
        MapSqlParameterSource parameters = baseParameters(countryCode.toUpperCase(), productType)
                .addValue("parentAreaId", parentAreaId)
                .addValue("areaType", normalizeType(areaType));
        String sql =
                """
            WITH latest_snapshot AS (
                SELECT snapshot_id, average_price national_average
                FROM fuel_price_statistics
                WHERE country = :country AND product_type = :productType
                  AND administrative_area_id IS NULL
                ORDER BY calculated_at DESC LIMIT 1
            ), ranked AS (
                SELECT f.*, a.name area_name, a.type area_type, a.parent_id,
                       RANK() OVER (ORDER BY average_price, a.name) cheapest_rank,
                       RANK() OVER (ORDER BY average_price DESC, a.name) expensive_rank
                FROM fuel_price_statistics f
                JOIN administrative_area a ON a.id = f.administrative_area_id
                WHERE f.snapshot_id = (SELECT snapshot_id FROM latest_snapshot)
                  AND f.country = :country AND f.product_type = :productType
                  AND a.parent_id IS NOT DISTINCT FROM CAST(:parentAreaId AS uuid)
                  AND (:areaType IS NULL OR a.type = :areaType)
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
                    rs.getObject("administrative_area_id", UUID.class),
                    rs.getString("area_name"),
                    rs.getString("area_type"),
                    rs.getObject("parent_id", UUID.class),
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
    public List<AdministrativeArea> findAreas(String countryCode, UUID parentAreaId, String areaType) {
        String sql =
                """
                SELECT a.id, a.country, a.type, a.name, a.parent_id, p.name parent_name
                FROM administrative_area a
                LEFT JOIN administrative_area p ON p.id = a.parent_id
                WHERE a.country = :country
                  AND a.parent_id IS NOT DISTINCT FROM CAST(:parentAreaId AS uuid)
                  AND (:areaType IS NULL OR a.type = :areaType)
                ORDER BY a.type, a.name
                """;
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("country", countryCode.toUpperCase(java.util.Locale.ROOT))
                        .addValue("parentAreaId", parentAreaId)
                        .addValue("areaType", normalizeType(areaType)),
                (rs, rowNum) -> new AdministrativeArea(
                        rs.getObject("id", UUID.class),
                        rs.getString("country"),
                        rs.getString("type"),
                        rs.getString("name"),
                        rs.getObject("parent_id", UUID.class),
                        rs.getString("parent_name")));
    }

    @Override
    public Optional<BigDecimal> stationPrice(UUID stationId, ProductType productType) {
        String sql =
                """
            SELECT hp.price FROM historical_product_price hp
            JOIN fuel_price_statistics f ON f.snapshot_id = hp.snapshot_id
              AND f.product_type = hp.product_type AND f.administrative_area_id IS NULL
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

        synchronizeAdministrativeAreas(snapshotId);

        String sql =
                """
            WITH source AS (
                SELECT hp.snapshot_id, s.country, hp.product_type, hp.station_id, hp.price
                FROM historical_product_price hp
                JOIN station s ON s.id = hp.station_id
                WHERE hp.snapshot_id = :snapshotId AND hp.price > 0
            ), scopes AS (
                SELECT snapshot_id, country, product_type, NULL::uuid administrative_area_id,
                       station_id, price FROM source
                UNION ALL
                SELECT s.snapshot_id, s.country, s.product_type, saa.administrative_area_id,
                       s.station_id, s.price
                FROM source s
                JOIN station_administrative_area saa ON saa.station_id = s.station_id
            )
            INSERT INTO fuel_price_statistics (
                id, snapshot_id, country, product_type, administrative_area_id,
                average_price, minimum_price, maximum_price,
                station_count, cheapest_station_id, most_expensive_station_id, calculated_at
            )
            SELECT gen_random_uuid(), snapshot_id, country, product_type, administrative_area_id,
                   AVG(price), MIN(price), MAX(price), COUNT(*),
                   (ARRAY_AGG(station_id ORDER BY price, station_id))[1],
                   (ARRAY_AGG(station_id ORDER BY price DESC, station_id))[1], NOW()
            FROM scopes
            GROUP BY snapshot_id, country, product_type, administrative_area_id
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource("snapshotId", snapshotId));
    }

    private void synchronizeAdministrativeAreas(UUID snapshotId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("snapshotId", snapshotId);
        jdbcTemplate.update(
                """
                INSERT INTO administrative_area (country, source, external_code, type, name, normalized_name)
                SELECT DISTINCT s.country, 'legacy-address', 'province:' || LOWER(BTRIM(s.province)),
                       'PROVINCE', BTRIM(s.province), LOWER(BTRIM(s.province))
                FROM historical_product_price hp JOIN station s ON s.id = hp.station_id
                WHERE hp.snapshot_id = :snapshotId AND NULLIF(BTRIM(s.province), '') IS NOT NULL
                ON CONFLICT (country, source, external_code) DO UPDATE
                SET name = EXCLUDED.name, normalized_name = EXCLUDED.normalized_name
                """,
                parameters);
        jdbcTemplate.update(
                """
                INSERT INTO administrative_area
                    (country, source, external_code, type, name, normalized_name, parent_id)
                SELECT DISTINCT s.country, 'legacy-address',
                       'municipality:' || LOWER(BTRIM(s.province)) || ':' || LOWER(BTRIM(COALESCE(s.municipality, s.locality))),
                       'MUNICIPALITY', BTRIM(COALESCE(s.municipality, s.locality)),
                       LOWER(BTRIM(COALESCE(s.municipality, s.locality))), p.id
                FROM historical_product_price hp JOIN station s ON s.id = hp.station_id
                JOIN administrative_area p ON p.country = s.country AND p.source = 'legacy-address'
                  AND p.external_code = 'province:' || LOWER(BTRIM(s.province))
                WHERE hp.snapshot_id = :snapshotId
                  AND NULLIF(BTRIM(s.province), '') IS NOT NULL
                  AND NULLIF(BTRIM(COALESCE(s.municipality, s.locality)), '') IS NOT NULL
                ON CONFLICT (country, source, external_code) DO UPDATE
                SET name = EXCLUDED.name, normalized_name = EXCLUDED.normalized_name,
                    parent_id = EXCLUDED.parent_id
                """,
                parameters);
        jdbcTemplate.update(
                """
                DELETE FROM station_administrative_area saa
                USING historical_product_price hp, administrative_area a
                WHERE hp.snapshot_id = :snapshotId AND hp.station_id = saa.station_id
                  AND a.id = saa.administrative_area_id AND a.source = 'legacy-address'
                """,
                parameters);
        jdbcTemplate.update(
                """
                INSERT INTO station_administrative_area (station_id, administrative_area_id)
                SELECT DISTINCT s.id, a.id
                FROM historical_product_price hp JOIN station s ON s.id = hp.station_id
                JOIN administrative_area a ON a.country = s.country AND a.source = 'legacy-address'
                  AND (a.external_code = 'province:' || LOWER(BTRIM(s.province))
                    OR a.external_code = 'municipality:' || LOWER(BTRIM(s.province)) || ':' ||
                       LOWER(BTRIM(COALESCE(s.municipality, s.locality))))
                WHERE hp.snapshot_id = :snapshotId
                ON CONFLICT DO NOTHING
                """,
                parameters);
    }

    private MapSqlParameterSource baseParameters(String countryCode, ProductType productType) {
        return new MapSqlParameterSource().addValue("country", countryCode).addValue("productType", productType.name());
    }

    private UUID resolveAreaId(String countryCode, GeographicScope scope) {
        if (scope.areaId() != null) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM administrative_area WHERE id = :id AND country = :country",
                    new MapSqlParameterSource("id", scope.areaId())
                            .addValue("country", countryCode.toUpperCase(java.util.Locale.ROOT)),
                    Integer.class);
            if (count == null || count == 0) {
                throw new IllegalArgumentException("areaId does not belong to countryCode");
            }
            return scope.areaId();
        }
        if (scope.level() == com.petrolprice.station_search_api.statistics.domain.GeographicLevel.NATIONAL) {
            return null;
        }
        String sql = scope.level() == com.petrolprice.station_search_api.statistics.domain.GeographicLevel.PROVINCE
                ? """
                  SELECT id FROM administrative_area
                  WHERE country = :country AND type = 'PROVINCE' AND normalized_name = LOWER(:name)
                  """
                : """
                  SELECT a.id FROM administrative_area a JOIN administrative_area p ON p.id = a.parent_id
                  WHERE a.country = :country AND a.type = 'MUNICIPALITY'
                    AND a.normalized_name = LOWER(:name) AND p.normalized_name = LOWER(:province)
                  """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("country", countryCode.toUpperCase(java.util.Locale.ROOT))
                .addValue("name", scope.name())
                .addValue("province", scope.province());
        List<UUID> ids = jdbcTemplate.query(sql, parameters, (rs, rowNum) -> rs.getObject("id", UUID.class));
        if (ids.size() != 1) {
            throw new IllegalArgumentException(ids.isEmpty()
                    ? "The legacy geographic area does not exist"
                    : "The legacy geographic area name is ambiguous; use areaId");
        }
        return ids.getFirst();
    }

    private String normalizeType(String areaType) {
        return areaType == null || areaType.isBlank()
                ? null
                : areaType.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private GeographicScope resolvedScope(ResultSet rs) throws SQLException {
        UUID areaId = rs.getObject("administrative_area_id", UUID.class);
        if (areaId == null) {
            return GeographicScope.national();
        }
        String type = rs.getString("area_type");
        com.petrolprice.station_search_api.statistics.domain.GeographicLevel legacyLevel =
                switch (type) {
                    case "PROVINCE" -> com.petrolprice.station_search_api.statistics.domain.GeographicLevel.PROVINCE;
                    case "MUNICIPALITY" ->
                        com.petrolprice.station_search_api.statistics.domain.GeographicLevel.MUNICIPALITY;
                    default -> null;
                };
        return GeographicScope.resolvedAdministrativeArea(
                areaId, type, legacyLevel, rs.getString("area_name"), rs.getString("parent_name"));
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
