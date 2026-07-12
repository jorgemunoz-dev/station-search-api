package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import com.petrolprice.station_search_api.application.port.out.StationSearchRepositoryPort;
import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsItem;
import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsQuery;
import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsResult;
import com.petrolprice.station_search_api.application.usecase.findstations.FindStationsSort;
import com.petrolprice.station_search_api.domain.model.OpeningPeriod;
import com.petrolprice.station_search_api.domain.model.ProductPrice;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper.StationSearchProjectionMapper;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.projection.OpeningPeriodProjection;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.projection.ProductPriceProjection;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.projection.StationRankingProjection;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.rowmapper.OpeningPeriodRowMapper;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.rowmapper.ProductPriceRowMapper;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.rowmapper.StationRankingRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PostgresStationSearchPersistenceAdapter implements StationSearchRepositoryPort {

    private static final int DEFAULT_LIMIT = 50;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final StationSearchProjectionMapper mapper;

    @Override
    public FindStationsResult search(FindStationsQuery query) {
        List<StationRankingProjection> rankedStations = findRankedStations(query);

        if (rankedStations.isEmpty()) {
            return new FindStationsResult(List.of());
        }

        List<UUID> stationIds = rankedStations.stream()
            .map(StationRankingProjection::id)
            .toList();

        Map<UUID, List<ProductPrice>> pricesByStation = findPricesByStation(stationIds);
        Map<UUID, List<OpeningPeriod>> openingPeriodsByStation = findOpeningPeriodsByStation(stationIds);

        List<FindStationsItem> items = rankedStations.stream()
            .map(rs -> mapper.toItem(
                rs,
                pricesByStation.getOrDefault(rs.id(), List.of()),
                openingPeriodsByStation.getOrDefault(rs.id(), List.of())
            ))
            .toList();

        return new FindStationsResult(items);
    }

    /**
     * Ranking query: nearby stations, filtered/sorted/paginated
     *
     * @param query
     * @return
     */
    private List<StationRankingProjection> findRankedStations(FindStationsQuery query) {
        String sql = """
            WITH nearby_stations AS (
                SELECT
                    s.*,
                    ST_Distance(
                        s.location,
                        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
                    ) AS distance_meters
                FROM station s
                WHERE ST_DWithin(
                    s.location,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMeters
                )
            ),
            filtered_prices AS (
                SELECT cp.station_id, cp.price
                FROM station_current_product_price cp
                %s
            ),
            ranked_stations AS (
                SELECT
                    ns.id,
                    ns.external_id,
                    ns.country,
                    ns.brand,
                    ns.street,
                    ns.postal_code,
                    ns.locality,
                    ns.municipality,
                    ns.province,
                    ns.location,
                    ns.distance_meters,
                    MIN(fp.price) AS station_min_price
                FROM nearby_stations ns
                %s filtered_prices fp ON fp.station_id = ns.id
                GROUP BY
                    ns.id, ns.external_id, ns.country, ns.brand,
                    ns.street, ns.postal_code, ns.locality,
                    ns.municipality, ns.province, ns.location,
                    ns.distance_meters
                ORDER BY %s
                LIMIT :limit
            )
            SELECT
                id,
                external_id,
                country,
                brand,
                street,
                postal_code,
                locality,
                municipality,
                province,
                ST_Y(location::geometry) AS latitude,
                ST_X(location::geometry) AS longitude,
                distance_meters
            FROM ranked_stations
            """.formatted(
            productTypeWhereClause(query),
            priceJoinType(query),
            RANK_ORDER_BY.get(query.sortBy())
        );

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("lat", query.latitude())
            .addValue("lng", query.longitude())
            .addValue("radiusMeters", query.radiusMeters())
            .addValue("limit", query.limit() != null ? query.limit() : DEFAULT_LIMIT);

        if (query.productType() != null) {
            params.addValue("productType", query.productType().name());
        }

        return jdbcTemplate.query(sql, params, new StationRankingRowMapper());
    }

    private String productTypeWhereClause(FindStationsQuery query) {
        return query.productType() == null
            ? ""
            : "WHERE cp.product_type = :productType";
    }

    private String priceJoinType(FindStationsQuery query) {
        return query.productType() != null ? "INNER JOIN" : "LEFT JOIN";
    }

    private static final Map<FindStationsSort, String> RANK_ORDER_BY = Map.of(
        FindStationsSort.PRICE,    "station_min_price ASC NULLS LAST, distance_meters ASC",
        FindStationsSort.DISTANCE, "distance_meters ASC"
    );


    /**
     * Prices query: all product prices for the ranked stations
     *
     * @param stationIds
     * @return
     */
    private Map<UUID, List<ProductPrice>> findPricesByStation(List<UUID> stationIds) {
        String sql = """
            SELECT
                cp.station_id,
                cp.product_type,
                cp.price
            FROM station_current_product_price cp
            WHERE cp.station_id IN (:stationIds)
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("stationIds", stationIds);

        List<ProductPriceProjection> rows = jdbcTemplate.query(sql, params, new ProductPriceRowMapper());

        return rows.stream()
            .collect(Collectors.groupingBy(
                ProductPriceProjection::stationId,
                Collectors.mapping(mapper::toProductPrice, Collectors.toList())
            ));
    }


    /**
     * Opening periods query: all opening periods for the ranked stations
     * @param stationIds
     * @return
     */
    private Map<UUID, List<OpeningPeriod>> findOpeningPeriodsByStation(List<UUID> stationIds) {
        String sql = """
        SELECT
            op.station_id,
            op.day_of_week,
            op.open_time,
            op.close_time
        FROM station_opening_period op
        WHERE op.station_id IN (:stationIds)
        """;

        Map<String, Object> params = new HashMap<>();
        params.put("stationIds", stationIds);

        List<OpeningPeriodProjection> rows =
            jdbcTemplate.query(sql, params, new OpeningPeriodRowMapper());

        return rows.stream()
            .collect(Collectors.groupingBy(OpeningPeriodProjection::stationId))
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue()
                    .stream()
                    .collect(Collectors.groupingBy(row ->
                        new OpeningHoursKey(row.openTime(), row.closeTime())
                    ))
                    .values()
                    .stream()
                    .map(mapper::toOpeningPeriod)
                    .toList()
            ));
    }

    private record OpeningHoursKey(
        LocalTime openTime,
        LocalTime closeTime
    ) {
    }

}