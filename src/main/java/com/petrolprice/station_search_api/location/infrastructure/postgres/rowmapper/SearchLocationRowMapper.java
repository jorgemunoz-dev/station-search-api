package com.petrolprice.station_search_api.location.infrastructure.postgres.rowmapper;

import com.petrolprice.station_search_api.location.infrastructure.postgres.projection.SearchLocationProjection;
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
                resultSet.getString("normalized_locality_name"),
                resultSet.getString("admin_area_1_name"),
                resultSet.getString("admin_area_1_code"),
                resultSet.getString("admin_area_2_name"),
                resultSet.getString("admin_area_2_code"),
                resultSet.getString("admin_area_3_name"),
                resultSet.getString("admin_area_3_code"),
                resultSet.getDouble("latitude"),
                resultSet.getDouble("longitude"));
    }
}
