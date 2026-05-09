package com.petrolprice.station_search_api.integration;

import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto.StationSnapshotMessage;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.StationEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.PostgresJPAStationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
public class StationSnapshotRabbitIT extends IntegrationTestBase {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PostgresJPAStationRepository stationJpaRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldConsumeStationSnapshotAndPersistStation() throws Exception {
        String message = """
            {
                "externalId":"4375",
                "country":"ES",
                "brand":"REPSOL",
                "openingPeriods":[
                    {"close":"22:00:00", "open":"07:00:00", "days":["MON","TUE","WED","THU","FRI"]},
                    {"close":"15:00:00", "open":"12:00:00", "days":["SAT","SUN"]}
                ],
                "address":{
                    "street":"AVENIDA CASTILLA LA MANCHA, 26",
                    "postalCode":"02250",
                    "locality":"ABENGIBRE",
                    "municipality":"Abengibre",
                    "province":"ALBACETE"
                },
                "location":{
                    "latitude":39.211417,
                    "longitude":-1.539167
                },
                "fuelPrices":[
                    {"stationProductType":"DIESEL_A", "price":1.599},
                    {"stationProductType":"DIESEL_B", "price":1.239},
                    {"stationProductType":"GASOLINE_95_E5", "price":1.449}
                ]
            }
        """;

        StationSnapshotMessage payload = objectMapper.readValue(
            message,
            StationSnapshotMessage.class
        );

        rabbitTemplate.convertAndSend(
            "energy.snapshot.events",
            "energy.snapshot.fuel.es.created",
            payload
        );

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> transactionTemplate.executeWithoutResult(status -> {
                List<StationEntity> stations = stationJpaRepository.findAll();

                assertThat(stations).hasSize(1);

                StationEntity station = stations.getFirst();

                assertThat(station.getExternalId()).isEqualTo("4375");
                assertThat(station.getBrand()).isEqualTo("REPSOL");
                assertThat(station.getCountry().name()).isEqualTo("ES");
                assertThat(station.getStreet()).isEqualTo("AVENIDA CASTILLA LA MANCHA, 26");
                assertThat(station.getCurrentFuelPrices()).hasSize(3);
                assertThat(station.getOpeningPeriods()).hasSize(7);
            }));
    }
}
