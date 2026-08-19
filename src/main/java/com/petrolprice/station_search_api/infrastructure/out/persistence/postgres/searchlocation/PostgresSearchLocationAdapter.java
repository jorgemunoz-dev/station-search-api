package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.searchlocation;

import com.petrolprice.station_search_api.application.port.out.SearchLocationPort;
import com.petrolprice.station_search_api.application.usecase.searchlocation.SearchLocationNormalizer;
import com.petrolprice.station_search_api.application.usecase.searchlocation.SearchLocationType;
import com.petrolprice.station_search_api.application.usecase.searchlocation.query.SearchLocationQuery;
import com.petrolprice.station_search_api.application.usecase.searchlocation.result.SearchLocationResult;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.searchlocation.projection.SearchLocationProjection;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.searchlocation.rowmapper.SearchLocationRowMapper;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.searchlocation.sql.SearchLocationSql;
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
