package com.petrolprice.station_search_api.statistics.infrastructure.postgres;

import com.petrolprice.station_search_api.statistics.application.port.out.CurrentPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.port.out.GeospatialPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.port.out.HistoricalPriceStatisticsRepository;
import com.petrolprice.station_search_api.statistics.application.query.CurrentStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.GeographicScope;
import com.petrolprice.station_search_api.statistics.application.query.HistoricalStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.query.RadiusStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.result.CurrentPriceStatistics;
import com.petrolprice.station_search_api.statistics.application.result.HistoricalPricePoint;
import com.petrolprice.station_search_api.statistics.application.result.RadiusPriceStatistics;
import com.petrolprice.station_search_api.statistics.application.result.RankedAreaStatistics;
import com.petrolprice.station_search_api.statistics.application.result.StationPricePoint;
import com.petrolprice.station_search_api.statistics.domain.GeographicLevel;
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
                GeospatialPriceStatisticsRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<CurrentPriceStatistics> current(CurrentStatisticsQuery query) {
        MapSqlParameterSource parameters = baseParameters(query.countryCode(), query.productType());
        String scopePredicate = scopePredicate(query.scope(), parameters, "s");
        String provincialAverage = query.scope().level() == GeographicLevel.MUNICIPALITY
                ? """(SELECT AVG(cp.price) FROM station_current_product_price cp JOIN station ps ON ps.id = cp.station_id
                       WHERE ps.country = :country AND cp.product_type = :productType AND cp.price > 0
                         AND LOWER(ps.province) = LOWER(:scopeProvince))"""
                : "NULL::numeric";

        String sql = """
            WITH scoped AS (
                SELECT s.id, s.external_id, s.brand, s.province, cp.price,
                       ST_Y(s.location::geometry) latitude,
                       ST_X(s.location::geometry) longitude,
                       cp.updated_at,
                       ROW_NUMBER() OVER (ORDER BY cp.price, s.id) cheapest_rank,
                       ROW_NUMBER() OVER (ORDER BY cp.price DESC, s.id) expensive_rank
                FROM station s
                JOIN station_current_product_price cp ON cp.station_id = s.id
                WHERE s.country = :country AND cp.product_type = :productType AND cp.price > 0 AND %s
            ), totals AS (
                SELECT AVG(price) average_price, MIN(price) minimum_price, MAX(price) maximum_price,
                       COUNT(*) station_count, MAX(updated_at) updated_at
                FROM scoped
            ), national AS (
                SELECT AVG(cp.price) average_price
                FROM station_current_product_price cp JOIN station s ON s.id = cp.station_id
                WHERE s.country = :country AND cp.product_type = :productType AND cp.price > 0
            )
            SELECT totals.*, national.average_price national_average, %s provincial_average,
                   cheap.id cheap_id, cheap.external_id cheap_external_id, cheap.brand cheap_brand,
                   cheap.price cheap_price, cheap.latitude cheap_latitude, cheap.longitude cheap_longitude,
                   expensive.id expensive_id, expensive.external_id expensive_external_id,
                   expensive.brand expensive_brand, expensive.price expensive_price,
                   expensive.latitude expensive_latitude, expensive.longitude expensive_longitude
            FROM totals CROSS JOIN national
            LEFT JOIN scoped cheap ON cheap.cheapest_rank = 1
            LEFT JOIN scoped expensive ON expensive.expensive_rank = 1
            """.formatted(scopePredicate, provincialAverage);

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
                    rs.getTimestamp("updated_at").toInstant()));
        });
    }

    @Override
    public List<HistoricalPricePoint> history(HistoricalStatisticsQuery query) {
        MapSqlParameterSource parameters = baseParameters(query.countryCode(), query.productType())
                .addValue("historyFrom", query.from().minusDays(30))
                .addValue("from", query.from())
                .addValue("to", query.to());
        String scopePredicate = scopePredicate(query.scope(), parameters, "s");
        String sql = """
            WITH station_daily AS (
                SELECT DISTINCT ON (hp.station_id, hp.observed_at::date)
                       hp.station_id, hp.observed_at::date observed_date, hp.price
                FROM historical_product_price hp
                JOIN station s ON s.id = hp.station_id
                WHERE s.country = :country AND hp.product_type = :productType AND hp.price > 0
                  AND hp.observed_at >= CAST(:historyFrom AS date)
                  AND hp.observed_at < (CAST(:to AS date) + INTERVAL '1 day')
                  AND %s
                ORDER BY hp.station_id, hp.observed_at::date, hp.observed_at DESC
            ), daily AS (
                SELECT observed_date, AVG(price) average_price, MIN(price) minimum_price,
                       MAX(price) maximum_price, COUNT(*) station_count
                FROM station_daily GROUP BY observed_date
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
            """.formatted(scopePredicate);
        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> new HistoricalPricePoint(
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
    public Optional<RadiusPriceStatistics> around(RadiusStatisticsQuery query) {
        MapSqlParameterSource parameters = baseParameters(query.countryCode(), query.productType())
                .addValue("latitude", query.latitude())
                .addValue("longitude", query.longitude())
                .addValue("radius", query.radiusMeters());
        String sql = """
            WITH nearby AS (
                SELECT s.id, s.external_id, s.brand, cp.price,
                       ST_Y(s.location::geometry) latitude, ST_X(s.location::geometry) longitude,
                       ST_Distance(s.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) distance_meters,
                       ROW_NUMBER() OVER (ORDER BY cp.price, s.id) cheapest_rank,
                       ROW_NUMBER() OVER (ORDER BY ST_Distance(s.location,
                           ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography), s.id) nearest_rank
                FROM station s JOIN station_current_product_price cp ON cp.station_id = s.id
                WHERE s.country = :country AND cp.product_type = :productType AND cp.price > 0
                  AND ST_DWithin(s.location,
                      ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radius)
            ), totals AS (
                SELECT COUNT(*) station_count, AVG(price) average_price,
                       MIN(price) minimum_price, MAX(price) maximum_price FROM nearby
            )
            SELECT totals.*, cheap.id cheap_id, cheap.external_id cheap_external_id,
                   cheap.brand cheap_brand, cheap.price cheap_price, cheap.latitude cheap_latitude,
                   cheap.longitude cheap_longitude, cheap.distance_meters cheap_distance,
                   nearest.id nearest_id, nearest.external_id nearest_external_id,
                   nearest.brand nearest_brand, nearest.price nearest_price, nearest.latitude nearest_latitude,
                   nearest.longitude nearest_longitude, nearest.distance_meters nearest_distance
            FROM totals
            LEFT JOIN nearby cheap ON cheap.cheapest_rank = 1
            LEFT JOIN nearby nearest ON nearest.nearest_rank = 1
            """;
        return jdbcTemplate.query(sql, parameters, rs -> {
            if (!rs.next() || rs.getLong("station_count") == 0) {
                return Optional.empty();
            }
            BigDecimal minimum = rs.getBigDecimal("minimum_price");
            BigDecimal maximum = rs.getBigDecimal("maximum_price");
            return Optional.of(new RadiusPriceStatistics(
                    query.countryCode(), query.productType(), query.latitude(), query.longitude(), query.radiusMeters(),
                    rs.getLong("station_count"), rs.getBigDecimal("average_price"), minimum, maximum,
                    maximum.subtract(minimum), station(rs, "cheap", rs.getDouble("cheap_distance")),
                    station(rs, "nearest", rs.getDouble("nearest_distance"))));
        });
    }

    @Override
    public List<RankedAreaStatistics> provinces(String countryCode, ProductType productType) {
        MapSqlParameterSource parameters = baseParameters(countryCode.toUpperCase(), productType);
        String sql = """
            WITH source AS (
                SELECT s.province, s.id, s.external_id, s.brand, cp.price,
                       ST_Y(s.location::geometry) latitude, ST_X(s.location::geometry) longitude
                FROM station s JOIN station_current_product_price cp ON cp.station_id = s.id
                WHERE s.country = :country AND cp.product_type = :productType AND cp.price > 0
                  AND s.province IS NOT NULL AND BTRIM(s.province) <> ''
            ), aggregate AS (
                SELECT province, AVG(price) average_price, MIN(price) minimum_price,
                       MAX(price) maximum_price, COUNT(*) station_count
                FROM source GROUP BY province
            ), cheapest AS (
                SELECT DISTINCT ON (province) province, id, external_id, brand, price, latitude, longitude
                FROM source ORDER BY province, price, id
            ), national AS (
                SELECT AVG(cp.price) average_price
                FROM station_current_product_price cp JOIN station s ON s.id = cp.station_id
                WHERE s.country = :country AND cp.product_type = :productType AND cp.price > 0
            ), ranked AS (
                SELECT aggregate.*,
                       RANK() OVER (ORDER BY average_price, province) cheapest_rank,
                       RANK() OVER (ORDER BY average_price DESC, province) expensive_rank
                FROM aggregate
            )
            SELECT ranked.*, national.average_price national_average,
                   cheapest.id cheap_id, cheapest.external_id cheap_external_id,
                   cheapest.brand cheap_brand, cheapest.price cheap_price,
                   cheapest.latitude cheap_latitude, cheapest.longitude cheap_longitude
            FROM ranked JOIN cheapest USING (province) CROSS JOIN national
            ORDER BY cheapest_rank
            """;
        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> {
            BigDecimal average = rs.getBigDecimal("average_price");
            return new RankedAreaStatistics(
                    rs.getString("province"), average, rs.getBigDecimal("minimum_price"),
                    rs.getBigDecimal("maximum_price"), rs.getLong("station_count"),
                    station(rs, "cheap", null), difference(average, rs.getBigDecimal("national_average")),
                    rs.getInt("cheapest_rank"), rs.getInt("expensive_rank"));
        });
    }

    @Override
    public Optional<BigDecimal> stationPrice(UUID stationId, ProductType productType) {
        String sql = """
            SELECT price FROM station_current_product_price
            WHERE station_id = :stationId AND product_type = :productType AND price > 0
            """;
        List<BigDecimal> prices = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("stationId", stationId)
                        .addValue("productType", productType.name()),
                (rs, rowNum) -> rs.getBigDecimal("price"));
        return prices.stream().findFirst();
    }

    private MapSqlParameterSource baseParameters(String countryCode, ProductType productType) {
        return new MapSqlParameterSource().addValue("country", countryCode).addValue("productType", productType.name());
    }

    private String scopePredicate(GeographicScope scope, MapSqlParameterSource parameters, String stationAlias) {
        return switch (scope.level()) {
            case NATIONAL -> "TRUE";
            case PROVINCE -> {
                parameters.addValue("scopeName", scope.name());
                yield "LOWER(" + stationAlias + ".province) = LOWER(:scopeName)";
            }
            case MUNICIPALITY -> {
                parameters.addValue("scopeName", scope.name());
                parameters.addValue("scopeProvince", scope.province());
                yield "LOWER(COALESCE(" + stationAlias + ".municipality, " + stationAlias
                        + ".locality)) = LOWER(:scopeName) AND LOWER(" + stationAlias
                        + ".province) = LOWER(:scopeProvince)";
            }
        };
    }

    private StationPricePoint station(ResultSet rs, String prefix, Double distance) throws SQLException {
        UUID id = rs.getObject(prefix + "_id", UUID.class);
        return id == null ? null : new StationPricePoint(
                id, rs.getString(prefix + "_external_id"), rs.getString(prefix + "_brand"),
                rs.getBigDecimal(prefix + "_price"), rs.getBigDecimal(prefix + "_latitude"),
                rs.getBigDecimal(prefix + "_longitude"), distance);
    }

    private BigDecimal difference(BigDecimal value, BigDecimal reference) {
        return value == null || reference == null ? null : value.subtract(reference);
    }
}
