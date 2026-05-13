package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa;

import com.petrolprice.station_search_api.domain.type.StationProductType;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.CurrentFuelPriceEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostgresJPACurrentPriceRepository extends JpaRepository<CurrentFuelPriceEntity, UUID> {

    void deleteByStationId(UUID stationId);

    void deleteByStationIdAndStationProductTypeNotIn(UUID stationId, List<StationProductType> productTypes);

    @Modifying
    @Query(
            value =
                    """
        INSERT INTO station_current_fuel_price (
            id,
            station_id,
            station_product_type,
            price,
            updated_at
        )
        VALUES (
            gen_random_uuid(),
            :stationId,
            :stationProductType,
            :price,
            now()
        )
        ON CONFLICT (station_id, station_product_type)
        DO UPDATE SET
            price = EXCLUDED.price,
            updated_at = EXCLUDED.updated_at
        """,
            nativeQuery = true)
    void upsertCurrentPrice(
            @Param("stationId") UUID stationId,
            @Param("stationProductType") String stationProductType,
            @Param("price") BigDecimal price);

    List<CurrentFuelPriceEntity> findByStationId(UUID stationId);
}
