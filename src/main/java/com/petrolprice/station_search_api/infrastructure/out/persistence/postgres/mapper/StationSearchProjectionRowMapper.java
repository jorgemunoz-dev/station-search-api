package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper;

import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.projection.StationSearchProjection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public class StationSearchProjectionRowMapper implements RowMapper<StationSearchProjection> {
    @Override
    public StationSearchProjection mapRow(ResultSet rs, int rowNum) throws SQLException {

        return new StationSearchProjection(
                UUID.fromString(rs.getString("id")),
                rs.getString("external_id"),
                rs.getString("country"),
                rs.getString("brand"),
                rs.getString("street"),
                rs.getString("postal_code"),
                rs.getString("locality"),
                rs.getString("municipality"),
                rs.getString("province"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                rs.getBigDecimal("distance_meters"),
                rs.getString("product_type"),
                rs.getBigDecimal("price"));
    }
}
