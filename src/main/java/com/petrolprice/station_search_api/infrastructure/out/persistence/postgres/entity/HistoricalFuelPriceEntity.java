package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity;

import com.petrolprice.station_search_api.domain.type.ProductType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "historical_product_price")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricalFuelPriceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 50)
    private ProductType productType;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal price;

    @CreationTimestamp
    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;
}
