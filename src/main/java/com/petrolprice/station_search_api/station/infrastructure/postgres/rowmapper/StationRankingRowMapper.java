package com.petrolprice.station_search_api.station.infrastructure.postgres.rowmapper;

import com.petrolprice.station_search_api.station.infrastructure.postgres.projection.StationRankingProjection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public class StationRankingRowMapper implements RowMapper<StationRankingProjection> {

    @Override
    public StationRankingProjection mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new StationRankingProjection(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("external_id"),
                resultSet.getString("country"),
                resultSet.getString("brand"),
                resultSet.getString("normalized_brand"),
                resultSet.getString("street"),
                resultSet.getString("postal_code"),
                resultSet.getString("locality_name"),
                resultSet.getString("normalized_locality_name"),
                resultSet.getString("admin_area_1_name"),
                resultSet.getString("admin_area_2_name"),
                resultSet.getString("admin_area_3_name"),
                resultSet.getDouble("latitude"),
                resultSet.getDouble("longitude"),
                resultSet.getObject("distance_meters", Double.class));
    }
}
