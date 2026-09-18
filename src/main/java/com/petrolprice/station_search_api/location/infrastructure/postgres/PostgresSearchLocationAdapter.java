package com.petrolprice.station_search_api.location.infrastructure.postgres;

import com.petrolprice.station_search_api.location.application.SearchLocationNormalizer;
import com.petrolprice.station_search_api.location.application.port.out.SearchLocationPort;
import com.petrolprice.station_search_api.location.application.query.SearchLocationQuery;
import com.petrolprice.station_search_api.location.application.result.SearchLocationResult;
import com.petrolprice.station_search_api.location.domain.SearchLocationType;
import com.petrolprice.station_search_api.location.infrastructure.postgres.projection.SearchLocationProjection;
import com.petrolprice.station_search_api.location.infrastructure.postgres.rowmapper.SearchLocationRowMapper;
import com.petrolprice.station_search_api.location.infrastructure.postgres.sql.SearchLocationSql;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresSearchLocationAdapter implements SearchLocationPort {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SearchLocationRowMapper rowMapper;
    private final SearchLocationNormalizer normalizer;

    @Override
    public List<SearchLocationResult> search(SearchLocationQuery query) {
        String normalizedTextQuery = normalizer.normalizeText(query.query());

        String normalizedPostalQuery = normalizer.normalizePostalCode(query.query());

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("countryCode", query.countryCode())
                .addValue("normalizedTextQuery", normalizedTextQuery)
                .addValue("normalizedPostalQuery", normalizedPostalQuery)
                .addValue("limit", query.limit());

        return jdbcTemplate.query(SearchLocationSql.SEARCH, parameters, rowMapper).stream()
                .map(this::toResult)
                .toList();
    }

    private SearchLocationResult toResult(SearchLocationProjection projection) {
        return new SearchLocationResult(
                SearchLocationType.valueOf(projection.suggestionType()),
                projection.primaryText(),
                projection.secondaryText(),
                projection.countryCode(),
                projection.postalCode(),
                projection.latitude(),
                projection.longitude());
    }
}
