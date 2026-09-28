package com.petrolprice.station_search_api.seo.infrastructure.postgres;

import com.petrolprice.station_search_api.seo.application.SitemapLocation;
import com.petrolprice.station_search_api.seo.application.port.out.SitemapLocationPort;
import java.sql.Timestamp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresSitemapLocationAdapter implements SitemapLocationPort {
    static final String FIND_LOCALITIES_WITH_CONTENT =
            """
            WITH locality_content AS (
                SELECT sl.country_code, sl.admin_area_1_name, sl.admin_area_2_name,
                       sl.normalized_locality_name, MAX(s.updated_at) AS content_updated_at
                FROM search_location sl
                JOIN station s
                  ON s.country = sl.country_code
                 AND s.postal_code = sl.postal_code
                GROUP BY sl.country_code, sl.admin_area_1_name, sl.admin_area_2_name,
                         sl.normalized_locality_name

                UNION ALL

                SELECT sl.country_code, sl.admin_area_1_name, sl.admin_area_2_name,
                       sl.normalized_locality_name, MAX(fps.calculated_at) AS content_updated_at
                FROM search_location sl
                JOIN fuel_price_statistics fps
                  ON fps.country = sl.country_code
                 AND fps.normalized_locality_name = sl.normalized_locality_name
                 AND fps.station_count > 0
                GROUP BY sl.country_code, sl.admin_area_1_name, sl.admin_area_2_name,
                         sl.normalized_locality_name
            )
            SELECT country_code, admin_area_1_name, admin_area_2_name, normalized_locality_name,
                   MAX(content_updated_at) AS content_updated_at
            FROM locality_content
            GROUP BY country_code, admin_area_1_name, admin_area_2_name, normalized_locality_name
            ORDER BY country_code, admin_area_1_name, admin_area_2_name, normalized_locality_name
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<SitemapLocation> findLocalitiesWithContent() {
        return jdbcTemplate.query(FIND_LOCALITIES_WITH_CONTENT, (resultSet, row) -> {
            Timestamp updatedAt = resultSet.getTimestamp("content_updated_at");
            return new SitemapLocation(
                    resultSet.getString("country_code"),
                    resultSet.getString("admin_area_1_name"),
                    resultSet.getString("admin_area_2_name"),
                    resultSet.getString("normalized_locality_name"),
                    updatedAt == null ? null : updatedAt.toInstant());
        });
    }
}
