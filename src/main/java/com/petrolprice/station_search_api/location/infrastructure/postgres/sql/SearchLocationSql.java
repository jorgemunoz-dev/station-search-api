package com.petrolprice.station_search_api.location.infrastructure.postgres.sql;

public final class SearchLocationSql {
    private SearchLocationSql() {}

    public static final String SEARCH = """
        WITH RECURSIVE hierarchy AS (
            SELECT sl.id search_id, area.id, area.parent_id, area.type, area.name,
                   area.normalized_name, area.source_code
            FROM search_location sl JOIN geographic_area area ON area.id=sl.locality_id
            WHERE sl.country_code=:countryCode
            UNION ALL
            SELECT h.search_id, parent.id, parent.parent_id, parent.type, parent.name,
                   parent.normalized_name, parent.source_code
            FROM hierarchy h JOIN geographic_area parent ON parent.id=h.parent_id
        ), metadata AS (
            SELECT sl.id, sl.country_code, sl.postal_code, sl.normalized_postal_code,
                   sl.location, sl.accuracy,
                   MAX(h.name) FILTER (WHERE h.type='LOCALITY') locality_name,
                   MAX(h.normalized_name) FILTER (WHERE h.type='LOCALITY') normalized_locality_name,
                   MAX(h.name) FILTER (WHERE h.type='ADMIN_AREA_1') admin_area_1_name,
                   MAX(h.source_code) FILTER (WHERE h.type='ADMIN_AREA_1') admin_area_1_code,
                   MAX(h.name) FILTER (WHERE h.type='ADMIN_AREA_2') admin_area_2_name,
                   MAX(h.source_code) FILTER (WHERE h.type='ADMIN_AREA_2') admin_area_2_code,
                   MAX(h.name) FILTER (WHERE h.type='ADMIN_AREA_3') admin_area_3_name,
                   MAX(h.source_code) FILTER (WHERE h.type='ADMIN_AREA_3') admin_area_3_code
            FROM search_location sl JOIN hierarchy h ON h.search_id=sl.id
            WHERE sl.country_code=:countryCode
            GROUP BY sl.id
        ), candidates AS (
            SELECT 'POSTAL_CODE' suggestion_type, postal_code primary_text,
                   CONCAT_WS(', ',locality_name,admin_area_2_name,admin_area_1_name) secondary_text,
                   *, CASE WHEN normalized_postal_code=:normalizedPostalQuery THEN 0 ELSE 1 END priority,
                   1.0::real score
            FROM metadata WHERE normalized_postal_code LIKE :normalizedPostalQuery || '%%'
            UNION ALL
            SELECT 'LOCALITY', locality_name,
                   CONCAT_WS(', ',admin_area_2_name,admin_area_1_name), *,
                   CASE WHEN normalized_locality_name=:normalizedTextQuery THEN 2
                        WHEN normalized_locality_name LIKE :normalizedTextQuery || '%%' THEN 3 ELSE 4 END,
                   similarity(normalized_locality_name,:normalizedTextQuery)
            FROM metadata WHERE normalized_locality_name LIKE :normalizedTextQuery || '%%'
                                OR normalized_locality_name % :normalizedTextQuery
        ), ranked AS (
            SELECT *, ROW_NUMBER() OVER (PARTITION BY suggestion_type, country_code,
                CASE WHEN suggestion_type='POSTAL_CODE' THEN normalized_postal_code ELSE normalized_locality_name END,
                COALESCE(admin_area_1_code,''),COALESCE(admin_area_2_code,''),COALESCE(admin_area_3_code,'')
                ORDER BY accuracy DESC NULLS LAST, id) duplicate_position
            FROM candidates
        )
        SELECT suggestion_type,primary_text,secondary_text,country_code,postal_code,
               locality_name station_locality_name,normalized_locality_name,
               admin_area_1_name,admin_area_1_code,admin_area_2_name,admin_area_2_code,
               admin_area_3_name,admin_area_3_code,
               ST_Y(location::geometry) latitude,ST_X(location::geometry) longitude
        FROM ranked WHERE duplicate_position=1
        ORDER BY priority,score DESC,primary_text LIMIT :limit
        """;
}
