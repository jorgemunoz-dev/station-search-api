package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.searchlocation.rowmapper;

import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.searchlocation.projection.SearchLocationProjection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class SearchLocationRowMapper implements RowMapper<SearchLocationProjection> {
    @Override
    public SearchLocationProjection mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SearchLocationProjection(
                resultSet.getString("suggestion_type"),
                resultSet.getString("primary_text"),
                resultSet.getString("secondary_text"),
                resultSet.getString("country_code"),
                resultSet.getString("postal_code"),
                resultSet.getDouble("latitude"),
                resultSet.getDouble("longitude"));
    }
}
