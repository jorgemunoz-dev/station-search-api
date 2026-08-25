package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity;

import com.petrolprice.station_search_api.domain.type.Day;
import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "station_opening_period")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationOpeningPeriodEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private Day day;

    @Column(name = "open_time", nullable = false)
    private LocalTime open;

    @Column(name = "close_time", nullable = false)
    private LocalTime close;
}
