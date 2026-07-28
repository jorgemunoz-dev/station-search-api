package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.rowmapper;

import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.projection.ProductPriceProjection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public class ProductPriceRowMapper implements RowMapper<ProductPriceProjection> {

    @Override
    public ProductPriceProjection mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ProductPriceProjection(
                UUID.fromString(rs.getString("station_id")), rs.getString("product_type"), rs.getBigDecimal("price"));
    }
}
