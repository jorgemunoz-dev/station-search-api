package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity;

import com.petrolprice.station_search_api.domain.type.StationProductType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "station_current_fuel_price")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentFuelPriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "station_product_type", nullable = false, length = 50)
    private StationProductType stationProductType;

    @Column(nullable = false,  precision = 10, scale = 3)
    private BigDecimal price;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
