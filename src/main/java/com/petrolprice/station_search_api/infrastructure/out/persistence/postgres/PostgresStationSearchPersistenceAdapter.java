package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import com.petrolprice.station_search_api.application.port.out.StationSearchRepositoryPort;
import com.petrolprice.station_search_api.application.usecase.findstations.result.FindStationsPage;
import com.petrolprice.station_search_api.application.usecase.findstations.query.FindStationsPageRequest;
import com.petrolprice.station_search_api.application.usecase.findstations.query.FindStationsQuery;
import com.petrolprice.station_search_api.application.usecase.findstations.query.FindStationsSort;
import com.petrolprice.station_search_api.application.usecase.findstations.result.FindStationsItem;
import com.petrolprice.station_search_api.application.usecase.findstations.result.FindStationsResult;
import com.petrolprice.station_search_api.application.usecase.findstations.searcharea.RadiusSearchArea;
import com.petrolprice.station_search_api.application.usecase.findstations.searcharea.ViewportSearchArea;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PostgresStationSearchPersistenceAdapter
    implements StationSearchRepositoryPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final StationSearchProjectionMapper mapper;

    @Override
    public FindStationsResult search(FindStationsQuery query) {
        FindStationsPageRequest pageRequest =
            query.pageRequest();

        List<StationRankingProjection> fetchedStations =
            findRankedStations(query);

        boolean hasNext =
            fetchedStations.size() > pageRequest.size();

        List<StationRankingProjection> rankedStations =
            hasNext
                ? List.copyOf(
                fetchedStations.subList(
                    0,
                    pageRequest.size()
                )
            )
                : fetchedStations;

        if (rankedStations.isEmpty()) {
            return FindStationsResult.empty(pageRequest);
        }

        List<UUID> stationIds = rankedStations.stream()
            .map(StationRankingProjection::id)
            .toList();

        Map<UUID, List<ProductPrice>> pricesByStation = findPricesByStation(stationIds);

        Map<UUID, List<OpeningPeriod>> openingPeriodsByStation = findOpeningPeriodsByStation(stationIds);

        List<FindStationsItem> items = rankedStations.stream()
            .map(station -> mapper.toItem(
                station,
                pricesByStation.getOrDefault(
                    station.id(),
                    List.of()
                ),
                openingPeriodsByStation.getOrDefault(
                    station.id(),
                    List.of()
                )
            ))
            .toList();

        FindStationsPage page = new FindStationsPage(
            pageRequest.page(),
            pageRequest.size(),
            items.size(),
            hasNext,
            pageRequest.page() > 0
        );

        return new FindStationsResult(
            items,
            page
        );
    }

    private List<StationRankingProjection> findRankedStations(
        FindStationsQuery query
    ) {
        return switch (query.searchArea()) {
            case RadiusSearchArea radius ->
                findRankedStationsByRadius(query, radius);

            case ViewportSearchArea viewport ->
                findRankedStationsByViewport(query, viewport);
        };
    }

    /*
     * RADIUS search:
     *
     * - Filters with ST_DWithin.
     * - Calculates distance from the search center.
     * - Supports PRICE and DISTANCE sorting.
     */
    private List<StationRankingProjection>
    findRankedStationsByRadius(
        FindStationsQuery query,
        RadiusSearchArea radius
    ) {
        String candidateStationsSql = """
            SELECT
                s.id,
                s.external_id,
                s.country,
                s.brand,
                s.street,
                s.postal_code,
                s.locality,
                s.municipality,
                s.province,
                s.location,
                ST_Distance(
                    s.location,
                    ST_SetSRID(
                        ST_MakePoint(:lng, :lat),
                        4326
                    )::geography
                ) AS distance_meters
            FROM station s
            WHERE ST_DWithin(
                s.location,
                ST_SetSRID(
                    ST_MakePoint(:lng, :lat),
                    4326
                )::geography,
                :radiusMeters
            )
            """;

        MapSqlParameterSource parameters =
            createCommonParameters(query)
                .addValue("lat", radius.latitude())
                .addValue("lng", radius.longitude())
                .addValue(
                    "radiusMeters",
                    radius.radiusMeters()
                );

        return executeRankingQuery(
            query,
            candidateStationsSql,
            radiusOrderBy(query.sortBy()),
            parameters
        );
    }

    /*
     * VIEWPORT search:
     *
     * - Filters stations located inside the visible map bounds.
     * - Does not calculate a distance.
     * - Supports viewports crossing the antimeridian.
     * - Only supports PRICE sorting.
     */
    private List<StationRankingProjection>
    findRankedStationsByViewport(
        FindStationsQuery query,
        ViewportSearchArea viewport
    ) {
        String candidateStationsSql = """
            SELECT
                s.id,
                s.external_id,
                s.country,
                s.brand,
                s.street,
                s.postal_code,
                s.locality,
                s.municipality,
                s.province,
                s.location,
                NULL::double precision AS distance_meters
            FROM station s
            WHERE (
                (
                    :west <= :east
                    AND ST_Intersects(
                        s.location,
                        ST_MakeEnvelope(
                            :west,
                            :south,
                            :east,
                            :north,
                            4326
                        )::geography
                    )
                )
                OR
                (
                    :west > :east
                    AND (
                        ST_Intersects(
                            s.location,
                            ST_MakeEnvelope(
                                :west,
                                :south,
                                180,
                                :north,
                                4326
                            )::geography
                        )
                        OR
                        ST_Intersects(
                            s.location,
                            ST_MakeEnvelope(
                                -180,
                                :south,
                                :east,
                                :north,
                                4326
                            )::geography
                        )
                    )
                )
            )
            """;

        MapSqlParameterSource parameters =
            createCommonParameters(query)
                .addValue("north", viewport.north())
                .addValue("south", viewport.south())
                .addValue("east", viewport.east())
                .addValue("west", viewport.west());

        return executeRankingQuery(
            query,
            candidateStationsSql,
            viewportOrderBy(query.sortBy()),
            parameters
        );
    }

    /*
     * Common query used after obtaining the geographical candidates.
     *
     * The geographical filter changes depending on the search area, but
     * product filtering, ranking and pagination remain the same.
     */
    private List<StationRankingProjection> executeRankingQuery(
        FindStationsQuery query,
        String candidateStationsSql,
        String orderBy,
        MapSqlParameterSource parameters
    ) {
        String sql = """
        WITH candidate_stations AS (
            %s
        ),
        filtered_prices AS (
            SELECT
                cp.station_id,
                cp.price
            FROM station_current_product_price cp
            %s
        ),
        ranked_stations AS (
            SELECT
                cs.id,
                cs.external_id,
                cs.country,
                cs.brand,
                cs.street,
                cs.postal_code,
                cs.locality,
                cs.municipality,
                cs.province,
                cs.location,
                cs.distance_meters,
                MIN(fp.price) AS station_min_price
            FROM candidate_stations cs
            %s filtered_prices fp
                ON fp.station_id = cs.id
            GROUP BY
                cs.id,
                cs.external_id,
                cs.country,
                cs.brand,
                cs.street,
                cs.postal_code,
                cs.locality,
                cs.municipality,
                cs.province,
                cs.location,
                cs.distance_meters
            ORDER BY %s
            LIMIT :fetchSize
            OFFSET :offset
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
        ORDER BY %s
        """.formatted(
            candidateStationsSql,
            productTypeWhereClause(query),
            priceJoinType(query),
            orderBy,
            orderBy
        );

        System.out.println(sql);

        return jdbcTemplate.query(
            sql,
            parameters,
            new StationRankingRowMapper()
        );
    }

    private MapSqlParameterSource createCommonParameters(
        FindStationsQuery query
    ) {
        FindStationsPageRequest pageRequest =
            query.pageRequest();

        MapSqlParameterSource parameters =
            new MapSqlParameterSource()
                .addValue(
                    "fetchSize",
                    pageRequest.fetchSize()
                )
                .addValue(
                    "offset",
                    pageRequest.offset()
                );

        if (query.productType() != null) {
            parameters.addValue(
                "productType",
                query.productType().name()
            );
        }

        return parameters;
    }

    private String productTypeWhereClause(
        FindStationsQuery query
    ) {
        if (query.productType() == null) {
            return "";
        }

        return "WHERE cp.product_type = :productType";
    }

    private String priceJoinType(
        FindStationsQuery query
    ) {
        if (query.productType() == null) {
            return "LEFT JOIN";
        }

        return "INNER JOIN";
    }

    private String radiusOrderBy(
        FindStationsSort sort
    ) {
        return switch (sort) {
            case PRICE ->
                """
                station_min_price ASC NULLS LAST,
                distance_meters ASC,
                id ASC
                """;

            case DISTANCE ->
                """
                distance_meters ASC,
                id ASC
                """;
        };
    }

    private String viewportOrderBy(
        FindStationsSort sort
    ) {
        if (sort != FindStationsSort.PRICE) {
            throw new IllegalArgumentException(
                "DISTANCE sorting is not supported for VIEWPORT searches"
            );
        }

        return """
            station_min_price ASC NULLS LAST,
            id ASC
            """;
    }

    /*
     * Loads all current prices for the selected stations.
     */
    private Map<UUID, List<ProductPrice>> findPricesByStation(
        List<UUID> stationIds
    ) {
        String sql = """
            SELECT
                cp.station_id,
                cp.product_type,
                cp.price
            FROM station_current_product_price cp
            WHERE cp.station_id IN (:stationIds)
            """;

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stationIds", stationIds);

        List<ProductPriceProjection> rows = jdbcTemplate.query(
            sql,
            parameters,
            new ProductPriceRowMapper()
        );

        return rows.stream()
            .collect(Collectors.groupingBy(
                ProductPriceProjection::stationId,
                Collectors.mapping(
                    mapper::toProductPrice,
                    Collectors.toList()
                )
            ));
    }

    /*
     * Loads all opening periods for the selected stations.
     */
    private Map<UUID, List<OpeningPeriod>>
    findOpeningPeriodsByStation(
        List<UUID> stationIds
    ) {
        String sql = """
            SELECT
                op.station_id,
                op.day_of_week,
                op.open_time,
                op.close_time
            FROM station_opening_period op
            WHERE op.station_id IN (:stationIds)
            """;

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stationIds", stationIds);

        List<OpeningPeriodProjection> rows =
            jdbcTemplate.query(
                sql,
                parameters,
                new OpeningPeriodRowMapper()
            );

        return rows.stream()
            .collect(Collectors.groupingBy(
                OpeningPeriodProjection::stationId
            ))
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue()
                    .stream()
                    .collect(Collectors.groupingBy(
                        row -> new OpeningHoursKey(
                            row.openTime(),
                            row.closeTime()
                        )
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