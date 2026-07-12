package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.rowmapper;

import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.projection.StationRankingProjection;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class StationRankingRowMapper implements RowMapper<StationRankingProjection> {

    @Override
    public StationRankingProjection mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new StationRankingProjection(
            UUID.fromString(rs.getString("id")),
            rs.getString("external_id"),
            rs.getString("country"),
            rs.getString("brand"),
            rs.getString("street"),
            rs.getString("postal_code"),
            rs.getString("locality"),
            rs.getString("municipality"),
            rs.getString("province"),
            rs.getDouble("latitude"),
            rs.getDouble("longitude"),
            rs.getDouble("distance_meters")
        );
    }
}
