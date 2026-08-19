package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.searchlocation.sql;

public class SearchLocationSql {
    private SearchLocationSql() {}

    public static final String SEARCH =
            """
        WITH postal_suggestions AS (
            SELECT
                'POSTAL_CODE' AS suggestion_type,
                postal_code AS primary_text,
                CONCAT_WS(
                    ', ',
                    locality_name,
                    admin_area_2_name,
                    admin_area_1_name
                ) AS secondary_text,
                country_code,
                postal_code,
                ST_Y(location::geometry) AS latitude,
                ST_X(location::geometry) AS longitude,
                CASE
                    WHEN normalized_postal_code = :normalizedPostalQuery
                        THEN 0
                    ELSE 1
                END AS match_priority,
                1.0::real AS similarity_score
            FROM search_location
            WHERE country_code = :countryCode
              AND normalized_postal_code LIKE :normalizedPostalQuery || '%%'
        ),
        ranked_localities AS (
            SELECT
                locality_name,
                normalized_locality_name,
                admin_area_1_name,
                admin_area_1_code,
                admin_area_2_name,
                admin_area_2_code,
                admin_area_3_name,
                admin_area_3_code,
                country_code,
                location,
                accuracy,
                CASE
                    WHEN normalized_locality_name = :normalizedTextQuery
                        THEN 2
                    WHEN normalized_locality_name LIKE :normalizedTextQuery || '%%'
                        THEN 3
                    ELSE 4
                END AS match_priority,
                similarity(
                    normalized_locality_name,
                    :normalizedTextQuery
                ) AS similarity_score,
                ROW_NUMBER() OVER (
                    PARTITION BY
                        country_code,
                        normalized_locality_name,
                        COALESCE(admin_area_1_code, ''),
                        COALESCE(admin_area_2_code, ''),
                        COALESCE(admin_area_3_code, '')
                    ORDER BY accuracy DESC NULLS LAST
                ) AS duplicate_position
            FROM search_location
            WHERE country_code = :countryCode
              AND (
                  normalized_locality_name LIKE :normalizedTextQuery || '%%'
                  OR normalized_locality_name % :normalizedTextQuery
              )
        ),
        locality_suggestions AS (
            SELECT
                'LOCALITY' AS suggestion_type,
                locality_name AS primary_text,
                CONCAT_WS(
                    ', ',
                    admin_area_2_name,
                    admin_area_1_name
                ) AS secondary_text,
                country_code,
                NULL::varchar AS postal_code,
                ST_Y(location::geometry) AS latitude,
                ST_X(location::geometry) AS longitude,
                match_priority,
                similarity_score
            FROM ranked_localities
            WHERE duplicate_position = 1
        ),
        combined_suggestions AS (
            SELECT *
            FROM postal_suggestions

            UNION ALL

            SELECT *
            FROM locality_suggestions
        )
        SELECT
            suggestion_type,
            primary_text,
            secondary_text,
            country_code,
            postal_code,
            latitude,
            longitude
        FROM combined_suggestions
        ORDER BY
            match_priority,
            similarity_score DESC,
            primary_text
        LIMIT :limit
        """;
}
