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
                SELECT locality.country_code, admin1.name admin_area_1_name,
                       admin2.name admin_area_2_name, locality.normalized_name normalized_locality_name,
                       MAX(s.updated_at) AS content_updated_at
                FROM station s JOIN geographic_area locality ON locality.id=s.locality_id
                LEFT JOIN geographic_area admin1 ON admin1.id=s.admin_area_1_id
                LEFT JOIN geographic_area admin2 ON admin2.id=s.admin_area_2_id
                GROUP BY locality.country_code, admin1.name, admin2.name, locality.normalized_name

                UNION ALL

                SELECT locality.country_code, admin1.name, admin2.name, locality.normalized_name,
                       MAX(fps.calculated_at)
                FROM fuel_price_statistics fps
                JOIN geographic_area locality ON locality.id=fps.area_id AND locality.type='LOCALITY'
                JOIN station s ON s.locality_id=locality.id
                LEFT JOIN geographic_area admin1 ON admin1.id=s.admin_area_1_id
                LEFT JOIN geographic_area admin2 ON admin2.id=s.admin_area_2_id
                WHERE fps.station_count > 0
                GROUP BY locality.country_code, admin1.name, admin2.name, locality.normalized_name
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
