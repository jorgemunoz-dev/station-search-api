package com.petrolprice.station_search_api.statistics.infrastructure.postgres;

import com.petrolprice.station_search_api.location.application.SearchLocationNormalizer;
import com.petrolprice.station_search_api.statistics.application.port.out.*;
import com.petrolprice.station_search_api.statistics.application.query.*;
import com.petrolprice.station_search_api.statistics.application.result.*;
import com.petrolprice.station_search_api.statistics.domain.ProductType;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresFuelPriceStatisticsRepository implements CurrentPriceStatisticsRepository,
        HistoricalPriceStatisticsRepository, StatisticsCalculationRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SearchLocationNormalizer normalizer;

    @Override
    public Optional<CurrentPriceStatistics> current(CurrentStatisticsQuery query) {
        Long areaId = resolveArea(query.countryCode(), query.scope());
        if (areaId == null) return Optional.empty();
        var parameters = baseParameters(query.countryCode(), query.productType()).addValue("areaId", areaId);
        String sql = """
            WITH selected AS (
              SELECT f.* FROM fuel_price_statistics f
              WHERE f.area_id=:areaId AND f.product_type=:productType
              ORDER BY f.calculated_at DESC LIMIT 1
            ), country_area AS (
              SELECT id FROM geographic_area WHERE country_code=:country AND type='COUNTRY'
            ), country_statistics AS (
              SELECT average_price FROM fuel_price_statistics
              WHERE snapshot_id=(SELECT snapshot_id FROM selected) AND area_id=(SELECT id FROM country_area)
                AND product_type=:productType
            ), hierarchy AS (
              WITH RECURSIVE parents AS (
                SELECT ga.id,ga.parent_id,ga.type,ga.name FROM geographic_area ga WHERE ga.id=:areaId
                UNION ALL SELECT p.id,p.parent_id,p.type,p.name FROM geographic_area p JOIN parents c ON p.id=c.parent_id
              ) SELECT * FROM parents
            ), admin2_statistics AS (
              SELECT f.average_price FROM fuel_price_statistics f
              JOIN hierarchy h ON h.id=f.area_id AND h.type='ADMIN_AREA_2'
              WHERE f.snapshot_id=(SELECT snapshot_id FROM selected) AND f.product_type=:productType
            )
            SELECT selected.*, country_statistics.average_price country_average,
                   admin2_statistics.average_price admin_area_2_average,
                   (SELECT name FROM hierarchy WHERE type='ADMIN_AREA_1') admin_area_1_name,
                   (SELECT name FROM hierarchy WHERE type='ADMIN_AREA_2') admin_area_2_name,
                   (SELECT name FROM hierarchy WHERE type='ADMIN_AREA_3') admin_area_3_name,
                   selected.cheapest_station_id cheap_id,selected.minimum_price cheap_price,
                   cheap.external_id cheap_external_id,cheap.brand cheap_brand,
                   ST_Y(cheap.location::geometry) cheap_latitude,ST_X(cheap.location::geometry) cheap_longitude,
                   selected.most_expensive_station_id expensive_id,selected.maximum_price expensive_price,
                   expensive.external_id expensive_external_id,expensive.brand expensive_brand,
                   ST_Y(expensive.location::geometry) expensive_latitude,ST_X(expensive.location::geometry) expensive_longitude
            FROM selected CROSS JOIN country_statistics LEFT JOIN admin2_statistics ON TRUE
            JOIN station cheap ON cheap.id=selected.cheapest_station_id
            JOIN station expensive ON expensive.id=selected.most_expensive_station_id
            """;
        return jdbcTemplate.query(sql, parameters, rs -> {
            if (!rs.next() || rs.getLong("station_count") == 0) return Optional.empty();
            BigDecimal average = rs.getBigDecimal("average_price");
            return Optional.of(new CurrentPriceStatistics(query.countryCode(), query.productType(), resolvedScope(rs), average,
                    rs.getBigDecimal("minimum_price"), rs.getBigDecimal("maximum_price"), rs.getLong("station_count"),
                    station(rs,"cheap",null), station(rs,"expensive",null),
                    difference(average,rs.getBigDecimal("country_average")),
                    difference(average,rs.getBigDecimal("admin_area_2_average")),
                    rs.getTimestamp("calculated_at").toInstant()));
        });
    }

    @Override
    public List<HistoricalPricePoint> history(HistoricalStatisticsQuery query) {
        Long areaId = resolveArea(query.countryCode(), query.scope());
        if (areaId == null) return List.of();
        var p=baseParameters(query.countryCode(),query.productType()).addValue("areaId",areaId)
                .addValue("historyFrom",query.from().minusDays(30)).addValue("from",query.from()).addValue("to",query.to());
        String sql="""
            WITH daily AS (
              SELECT DISTINCT ON (calculated_at::date) calculated_at::date observed_date,average_price,
                     minimum_price,maximum_price,station_count
              FROM fuel_price_statistics WHERE area_id=:areaId AND product_type=:productType
                AND calculated_at>=CAST(:historyFrom AS date) AND calculated_at<(CAST(:to AS date)+INTERVAL '1 day')
              ORDER BY calculated_at::date,calculated_at DESC
            )
            SELECT d.*,AVG(d.average_price) OVER() period_average_price,MIN(d.minimum_price) OVER() period_minimum_price,
              MAX(d.maximum_price) OVER() period_maximum_price,d.average_price-d1.average_price change_1d,
              d.average_price-d7.average_price change_7d,d.average_price-d30.average_price change_30d,
              100*(d.average_price-d1.average_price)/NULLIF(d1.average_price,0) percentage_1d,
              100*(d.average_price-d7.average_price)/NULLIF(d7.average_price,0) percentage_7d,
              100*(d.average_price-d30.average_price)/NULLIF(d30.average_price,0) percentage_30d
            FROM daily d LEFT JOIN daily d1 ON d1.observed_date=d.observed_date-1
              LEFT JOIN daily d7 ON d7.observed_date=d.observed_date-7
              LEFT JOIN daily d30 ON d30.observed_date=d.observed_date-30
            WHERE d.observed_date BETWEEN CAST(:from AS date) AND CAST(:to AS date) ORDER BY d.observed_date
            """;
        return jdbcTemplate.query(sql,p,(rs,n)->new HistoricalPricePoint(rs.getDate("observed_date").toLocalDate(),
                rs.getBigDecimal("average_price"),rs.getBigDecimal("minimum_price"),rs.getBigDecimal("maximum_price"),
                rs.getLong("station_count"),rs.getBigDecimal("period_average_price"),rs.getBigDecimal("period_minimum_price"),
                rs.getBigDecimal("period_maximum_price"),rs.getBigDecimal("change_1d"),rs.getBigDecimal("change_7d"),
                rs.getBigDecimal("change_30d"),rs.getBigDecimal("percentage_1d"),rs.getBigDecimal("percentage_7d"),rs.getBigDecimal("percentage_30d")));
    }

    @Override
    public List<RankedLocalityStatistics> localities(String countryCode, ProductType productType,
                                                      String adminArea1,String adminArea2,String adminArea3) {
        var p=baseParameters(countryCode,productType).addValue("admin1",normalize(adminArea1))
                .addValue("admin2",normalize(adminArea2)).addValue("admin3",normalize(adminArea3));
        String sql="""
            WITH RECURSIVE latest AS (
              SELECT f.snapshot_id,f.average_price country_average FROM fuel_price_statistics f
              JOIN geographic_area ga ON ga.id=f.area_id AND ga.type='COUNTRY' AND ga.country_code=:country
              WHERE f.product_type=:productType ORDER BY f.calculated_at DESC LIMIT 1
            ), ancestry AS (
              SELECT ga.id locality_id,ga.id,ga.parent_id,ga.type,ga.name,ga.normalized_name
              FROM geographic_area ga WHERE ga.type='LOCALITY' AND ga.country_code=:country
              UNION ALL SELECT a.locality_id,p.id,p.parent_id,p.type,p.name,p.normalized_name
              FROM ancestry a JOIN geographic_area p ON p.id=a.parent_id
            ), metadata AS (
              SELECT locality_id,MAX(name) FILTER(WHERE type='LOCALITY') locality_name,
                MAX(normalized_name) FILTER(WHERE type='LOCALITY') normalized_locality_name,
                MAX(name) FILTER(WHERE type='ADMIN_AREA_1') admin_area_1_name,
                MAX(normalized_name) FILTER(WHERE type='ADMIN_AREA_1') admin1_normalized,
                MAX(name) FILTER(WHERE type='ADMIN_AREA_2') admin_area_2_name,
                MAX(normalized_name) FILTER(WHERE type='ADMIN_AREA_2') admin2_normalized,
                MAX(name) FILTER(WHERE type='ADMIN_AREA_3') admin_area_3_name,
                MAX(normalized_name) FILTER(WHERE type='ADMIN_AREA_3') admin3_normalized
              FROM ancestry GROUP BY locality_id
            ), ranked AS (
              SELECT f.*,m.*,RANK() OVER(ORDER BY average_price,m.locality_name) cheapest_rank,
                RANK() OVER(ORDER BY average_price DESC,m.locality_name) expensive_rank
              FROM fuel_price_statistics f JOIN metadata m ON m.locality_id=f.area_id
              WHERE f.snapshot_id=(SELECT snapshot_id FROM latest) AND f.product_type=:productType
                AND (:admin1='' OR m.admin1_normalized=:admin1) AND (:admin2='' OR m.admin2_normalized=:admin2)
                AND (:admin3='' OR m.admin3_normalized=:admin3)
            )
            SELECT ranked.*,latest.country_average,cheap.id cheap_id,cheap.external_id cheap_external_id,
              cheap.brand cheap_brand,ranked.minimum_price cheap_price,ST_Y(cheap.location::geometry) cheap_latitude,
              ST_X(cheap.location::geometry) cheap_longitude
            FROM ranked CROSS JOIN latest JOIN station cheap ON cheap.id=ranked.cheapest_station_id ORDER BY cheapest_rank
            """;
        return jdbcTemplate.query(sql,p,(rs,n)->{var avg=rs.getBigDecimal("average_price");return new RankedLocalityStatistics(
                rs.getString("normalized_locality_name"),rs.getString("locality_name"),rs.getString("admin_area_1_name"),
                rs.getString("admin_area_2_name"),rs.getString("admin_area_3_name"),avg,rs.getBigDecimal("minimum_price"),
                rs.getBigDecimal("maximum_price"),rs.getLong("station_count"),station(rs,"cheap",null),
                difference(avg,rs.getBigDecimal("country_average")),rs.getInt("cheapest_rank"),rs.getInt("expensive_rank"));});
    }

    @Override
    public Optional<BigDecimal> stationPrice(UUID stationId, ProductType productType) {
        String sql="""SELECT hp.price FROM historical_product_price hp JOIN fuel_price_statistics f
          ON f.snapshot_id=hp.snapshot_id AND f.product_type=hp.product_type
          JOIN geographic_area ga ON ga.id=f.area_id AND ga.type='COUNTRY'
          WHERE hp.station_id=:stationId AND hp.product_type=:productType AND hp.price>0
          ORDER BY f.calculated_at DESC LIMIT 1""";
        return jdbcTemplate.query(sql,new MapSqlParameterSource("stationId",stationId).addValue("productType",productType.name()),
                (rs,n)->rs.getBigDecimal("price")).stream().findFirst();
    }

    @Override
    public void replaceForSnapshot(UUID snapshotId) {
        jdbcTemplate.update("DELETE FROM fuel_price_statistics WHERE snapshot_id=:snapshotId",new MapSqlParameterSource("snapshotId",snapshotId));
        String sql="""
          WITH source AS (
            SELECT hp.snapshot_id,hp.product_type,hp.station_id,hp.price,hp.observed_at,
              country.id country_id,s.admin_area_1_id,s.admin_area_2_id,s.admin_area_3_id,s.locality_id
            FROM historical_product_price hp JOIN station s ON s.id=hp.station_id
            JOIN geographic_area country ON country.country_code=s.country AND country.type='COUNTRY'
            WHERE hp.snapshot_id=:snapshotId AND hp.price>0 AND s.locality_id IS NOT NULL
          ), scopes AS (
            SELECT snapshot_id,product_type,station_id,price,observed_at,area_id
            FROM source CROSS JOIN LATERAL unnest(ARRAY[country_id,admin_area_1_id,admin_area_2_id,admin_area_3_id,locality_id]) area_id
            WHERE area_id IS NOT NULL
          )
          INSERT INTO fuel_price_statistics(id,snapshot_id,area_id,product_type,average_price,minimum_price,maximum_price,
            station_count,cheapest_station_id,most_expensive_station_id,calculated_at)
          SELECT gen_random_uuid(),snapshot_id,area_id,product_type,AVG(price),MIN(price),MAX(price),COUNT(*),
            (ARRAY_AGG(station_id ORDER BY price,station_id))[1],(ARRAY_AGG(station_id ORDER BY price DESC,station_id))[1],MAX(observed_at)
          FROM scopes GROUP BY snapshot_id,area_id,product_type
          """;
        jdbcTemplate.update(sql,new MapSqlParameterSource("snapshotId",snapshotId));
    }

    private Long resolveArea(String country, GeographicScope scope) {
        var p=new MapSqlParameterSource("country",country.toUpperCase()).addValue("a1",normalize(scope.adminArea1Name()))
                .addValue("a2",normalize(scope.adminArea2Name())).addValue("a3",normalize(scope.adminArea3Name()));
        String sql="""
          SELECT COALESCE(a3.id,a2.id,a1.id,c.id) FROM geographic_area c
          LEFT JOIN geographic_area a1 ON a1.parent_id=c.id AND a1.type='ADMIN_AREA_1' AND a1.normalized_name=:a1
          LEFT JOIN geographic_area a2 ON a2.parent_id=a1.id AND a2.type='ADMIN_AREA_2' AND a2.normalized_name=:a2
          LEFT JOIN geographic_area a3 ON a3.parent_id=a2.id AND a3.type='ADMIN_AREA_3' AND a3.normalized_name=:a3
          WHERE c.country_code=:country AND c.type='COUNTRY'
            AND (:a1='' OR a1.id IS NOT NULL) AND (:a2='' OR a2.id IS NOT NULL) AND (:a3='' OR a3.id IS NOT NULL)
          LIMIT 1""";
        return jdbcTemplate.query(sql,p,rs->rs.next()?rs.getLong(1):null);
    }
    private MapSqlParameterSource baseParameters(String country,ProductType product){return new MapSqlParameterSource("country",country.toUpperCase()).addValue("productType",product.name());}
    private String normalize(String value){return normalizer.normalizeText(value);}
    private GeographicScope resolvedScope(ResultSet rs)throws SQLException{return new GeographicScope(rs.getString("admin_area_1_name"),rs.getString("admin_area_2_name"),rs.getString("admin_area_3_name"));}
    private StationPricePoint station(ResultSet rs,String prefix,Double distance)throws SQLException{UUID id=rs.getObject(prefix+"_id",UUID.class);return id==null?null:new StationPricePoint(id,rs.getString(prefix+"_external_id"),rs.getString(prefix+"_brand"),rs.getBigDecimal(prefix+"_price"),rs.getBigDecimal(prefix+"_latitude"),rs.getBigDecimal(prefix+"_longitude"),distance);}
    private BigDecimal difference(BigDecimal value,BigDecimal reference){return value==null||reference==null?null:value.subtract(reference);}
}
