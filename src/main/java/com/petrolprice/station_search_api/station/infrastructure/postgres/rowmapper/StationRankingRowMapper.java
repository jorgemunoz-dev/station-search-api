package com.petrolprice.station_search_api.station.infrastructure.postgres.rowmapper;
import com.petrolprice.station_search_api.station.infrastructure.postgres.projection.StationRankingProjection;
import java.sql.*; import java.util.UUID; import org.springframework.jdbc.core.RowMapper;
public class StationRankingRowMapper implements RowMapper<StationRankingProjection> {
 public StationRankingProjection mapRow(ResultSet r,int n)throws SQLException{return new StationRankingProjection(
  r.getObject("id",UUID.class),r.getString("external_id"),r.getString("country"),r.getString("brand"),r.getString("normalized_brand"),
  r.getString("street"),r.getString("postal_code"),r.getString("locality_name"),r.getString("normalized_locality_name"),
  r.getString("admin_area_1_name"),r.getString("admin_area_2_name"),r.getString("admin_area_3_name"),r.getDouble("latitude"),r.getDouble("longitude"),r.getObject("distance_meters",Double.class));}
}
