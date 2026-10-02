package com.petrolprice.station_search_api.station.infrastructure.postgres;

import com.petrolprice.station_search_api.location.application.SearchLocationNormalizer;
import com.petrolprice.station_search_api.station.domain.model.Address;
import com.petrolprice.station_search_api.station.domain.type.Country;
import com.petrolprice.station_search_api.station.ingestion.application.port.out.GeographicAreaResolverPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresGeographicAreaResolver implements GeographicAreaResolverPort {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SearchLocationNormalizer normalizer;

    @Override
    public Resolution resolve(Country country, Address address) {
        var parameters = new MapSqlParameterSource()
                .addValue("country", country.name())
                .addValue("postal", normalizer.normalizePostalCode(address.getPostalCode()))
                .addValue("locality", normalizer.normalizeText(address.getLocalityName()))
                .addValue("admin1", normalizer.normalizeText(address.getAdminArea1Name()))
                .addValue("admin2", normalizer.normalizeText(address.getAdminArea2Name()))
                .addValue("admin3", normalizer.normalizeText(address.getAdminArea3Name()));
        List<Resolution> matches = jdbcTemplate.query(SQL, parameters, (rs, row) -> new Resolution(
                rs.getObject("admin_area_1_id", Long.class),
                rs.getObject("admin_area_2_id", Long.class),
                rs.getObject("admin_area_3_id", Long.class),
                rs.getLong("locality_id")));
        if (matches.isEmpty()) {
            return new Resolution(null, null, null, null);
        }
        return matches.getFirst();
    }

    static final String SQL = """
            WITH RECURSIVE postal_candidates AS (
                SELECT DISTINCT sl.locality_id
                FROM search_location sl
                WHERE sl.country_code=:country AND sl.normalized_postal_code=:postal
            ), hierarchy AS (
                SELECT ga.id locality_id, ga.id, ga.parent_id, ga.type, ga.normalized_name
                FROM geographic_area ga JOIN postal_candidates pc ON pc.locality_id=ga.id
                WHERE ga.active
                UNION ALL
                SELECT h.locality_id, parent.id, parent.parent_id, parent.type, parent.normalized_name
                FROM hierarchy h JOIN geographic_area parent ON parent.id=h.parent_id
            ), candidates AS (
                SELECT locality_id,
                       MAX(id) FILTER (WHERE type='ADMIN_AREA_1') admin_area_1_id,
                       MAX(id) FILTER (WHERE type='ADMIN_AREA_2') admin_area_2_id,
                       MAX(id) FILTER (WHERE type='ADMIN_AREA_3') admin_area_3_id,
                       MAX(normalized_name) FILTER (WHERE type='LOCALITY') locality_name,
                       MAX(normalized_name) FILTER (WHERE type='ADMIN_AREA_1') admin1_name,
                       MAX(normalized_name) FILTER (WHERE type='ADMIN_AREA_2') admin2_name,
                       MAX(normalized_name) FILTER (WHERE type='ADMIN_AREA_3') admin3_name
                FROM hierarchy GROUP BY locality_id
            )
            SELECT admin_area_1_id, admin_area_2_id, admin_area_3_id, locality_id
            FROM candidates
            WHERE (:locality='' OR locality_name=:locality OR EXISTS (
                     SELECT 1 FROM geographic_area ga WHERE ga.id=locality_id AND :locality=ANY(ga.aliases))
                     OR similarity(locality_name,:locality)>=0.85)
              AND (:admin1='' OR admin1_name=:admin1)
              AND (:admin2='' OR admin2_name=:admin2)
              AND (:admin3='' OR admin3_name=:admin3)
            ORDER BY (locality_name=:locality) DESC,
                     EXISTS (SELECT 1 FROM geographic_area ga WHERE ga.id=locality_id AND :locality=ANY(ga.aliases)) DESC,
                     similarity(locality_name,:locality) DESC, locality_id
            LIMIT 1
            """;
}
