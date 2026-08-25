package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.rowmapper;

import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.projection.OpeningPeriodProjection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public class OpeningPeriodRowMapper implements RowMapper<OpeningPeriodProjection> {

    @Override
    public OpeningPeriodProjection mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OpeningPeriodProjection(
                UUID.fromString(rs.getString("station_id")),
                rs.getString("day_of_week"),
                rs.getTime("open_time").toLocalTime(),
                rs.getTime("close_time").toLocalTime());
    }
}
