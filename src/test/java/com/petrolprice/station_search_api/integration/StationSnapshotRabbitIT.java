package com.petrolprice.station_search_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.petrolprice.station_search_api.domain.type.Country;
import com.petrolprice.station_search_api.domain.type.ProductType;
import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto.StationSnapshotMessage;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.CurrentFuelPriceEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.HistoricalFuelPriceEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.StationEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.JPACurrentPriceRepository;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.JPAHistoricalFuelPriceRepository;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.JPAStationRepository;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

public class StationSnapshotRabbitIT extends IntegrationTestBase {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JPAStationRepository stationJpaRepository;

    @Autowired
    private JPACurrentPriceRepository currentPriceRepository;

    @Autowired
    private JPAHistoricalFuelPriceRepository historicalFuelPriceRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        stationJpaRepository.deleteAll();
        currentPriceRepository.deleteAll();
        historicalFuelPriceRepository.deleteAll();
    }

    @Test
    void shouldConsumeStationSnapshotAndPersistStation() {
        sendSnapshot(
                """
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
                    {"productType":"DIESEL_A", "price":1.599},
                    {"productType":"DIESEL_B", "price":1.239},
                    {"productType":"GASOLINE_95_E5", "price":1.449}
                ]
            }
        """);

        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    Optional<StationEntity> stationOptional = transactionTemplate.execute(
                            status -> stationJpaRepository.findByExternalIdAndCountry("4375", Country.ES));

                    assertThat(stationOptional).isPresent();

                    StationEntity station = stationOptional.get();

                    assertThat(station.getExternalId()).isEqualTo("4375");
                    assertThat(station.getBrand()).isEqualTo("REPSOL");
                    assertThat(station.getCountry()).isEqualTo(Country.ES);
                    assertThat(station.getStreet()).isEqualTo("AVENIDA CASTILLA LA MANCHA, 26");
                    assertThat(station.getPostalCode()).isEqualTo("02250");
                    assertThat(station.getLocality()).isEqualTo("ABENGIBRE");
                    assertThat(station.getMunicipality()).isEqualTo("Abengibre");
                    assertThat(station.getProvince()).isEqualTo("ALBACETE");

                    List<CurrentFuelPriceEntity> currentPrices = transactionTemplate.execute(
                            status -> currentPriceRepository.findByStationId(station.getId()));

                    assertThat(currentPrices).hasSize(3);

                    assertThat(getCurrentPriceByProductType(currentPrices, ProductType.DIESEL_A)
                                    .getPrice())
                            .isEqualByComparingTo("1.599");

                    assertThat(getCurrentPriceByProductType(currentPrices, ProductType.DIESEL_B)
                                    .getPrice())
                            .isEqualByComparingTo("1.239");

                    assertThat(getCurrentPriceByProductType(currentPrices, ProductType.GASOLINE_95_E5)
                                    .getPrice())
                            .isEqualByComparingTo("1.449");

                    assertThat(station.getOpeningPeriods()).hasSize(7);

                    assertThat(station.getOpeningPeriods())
                            .extracting("open")
                            .containsOnly(LocalTime.of(7, 0), LocalTime.of(12, 0));

                    assertThat(station.getOpeningPeriods())
                            .extracting("close")
                            .containsOnly(LocalTime.of(22, 0), LocalTime.of(15, 0));

                    List<?> historicalPrices = transactionTemplate.execute(
                            status -> historicalFuelPriceRepository.findByStationId(station.getId()));

                    assertThat(historicalPrices).hasSize(3);
                });
    }

    @Test
    void shouldUpdateExistingStationWhenSnapshotChanges() {
        sendSnapshot(
                """
            {
                "externalId":"4375",
                "country":"ES",
                "brand":"REPSOL",
                "openingPeriods":[
                    {"close":"22:00:00", "open":"07:00:00", "days":["MON","TUE","WED","THU","FRI"]}
                ],
                "address":{
                    "street":"OLD STREET",
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
                    {"productType":"DIESEL_A", "price":1.599},
                    {"productType":"DIESEL_B", "price":1.599}
                ]
            }
        """);

        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    Optional<StationEntity> stationOptional = transactionTemplate.execute(
                            status -> stationJpaRepository.findByExternalIdAndCountry("4375", Country.ES));
                    assertThat(stationOptional).isPresent();
                    StationEntity station = stationOptional.get();

                    assertThat(station.getBrand()).isEqualTo("REPSOL");
                });

        sendSnapshot(
                """
            {
                "externalId":"4375",
                "country":"ES",
                "brand":"CEPSA",
                "openingPeriods":[
                    {"close":"23:00:00", "open":"08:00:00", "days":["MON","TUE","WED","THU"]},
                    {"close":"22:00:00", "open":"09:00:00", "days":["SAT","SUN"]}
                ],
                "address":{
                    "street":"NEW STREET",
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
                    {"productType":"DIESEL_A", "price":1.799},
                    {"productType":"GASOLINE_95_E5", "price":1.234}
                ]
            }
        """);

        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    Optional<StationEntity> stationOptional = transactionTemplate.execute(
                            status -> stationJpaRepository.findByExternalIdAndCountry("4375", Country.ES));
                    assertThat(stationOptional).isPresent();
                    StationEntity station = stationOptional.get();

                    assertThat(station.getBrand()).isEqualTo("CEPSA");
                    assertThat(station.getStreet()).isEqualTo("NEW STREET");

                    assertThat(station.getOpeningPeriods()).hasSize(6);

                    assertThat(station.getOpeningPeriods())
                            .extracting("open")
                            .containsOnly(LocalTime.of(8, 0), LocalTime.of(9, 0));

                    assertThat(station.getOpeningPeriods())
                            .extracting("close")
                            .containsOnly(LocalTime.of(23, 0), LocalTime.of(22, 0));

                    List<CurrentFuelPriceEntity> currentPrices =
                            currentPriceRepository.findByStationId(station.getId());

                    assertThat(currentPrices).hasSize(2);

                    assertThat(getCurrentPriceByProductType(currentPrices, ProductType.DIESEL_A)
                                    .getPrice())
                            .isEqualByComparingTo("1.799");

                    assertThat(getCurrentPriceByProductType(currentPrices, ProductType.GASOLINE_95_E5)
                                    .getPrice())
                            .isEqualByComparingTo("1.234");

                    List<HistoricalFuelPriceEntity> historicalPrices =
                            historicalFuelPriceRepository.findByStationId(station.getId());

                    assertThat(historicalPrices).hasSize(4);
                });
    }

    private CurrentFuelPriceEntity getCurrentPriceByProductType(
            List<CurrentFuelPriceEntity> currentPrices, ProductType productType) {
        return currentPrices.stream()
                .filter(currentPrice -> currentPrice.getProductType().equals(productType))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(String.format("Product type %s was not found", productType)));
    }

    private void sendSnapshot(String message) {
        StationSnapshotMessage payload = objectMapper.readValue(message, StationSnapshotMessage.class);

        rabbitTemplate.convertAndSend("energy.snapshot.events", "energy.snapshot.fuel.es.created", payload);
    }
}
