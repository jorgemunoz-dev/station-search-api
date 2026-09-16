package com.petrolprice.station_search_api.integration.support;

import com.petrolprice.station_search_api.application.usecase.stationSnapshots.command.CompleteStationPublishingCommand;
import com.petrolprice.station_search_api.application.usecase.stationSnapshots.command.ProcessStationSnapshotCommand;
import com.petrolprice.station_search_api.domain.model.Address;
import com.petrolprice.station_search_api.domain.model.GeoLocation;
import com.petrolprice.station_search_api.domain.model.OpeningPeriod;
import com.petrolprice.station_search_api.domain.model.ProductPrice;
import com.petrolprice.station_search_api.domain.model.Station;
import com.petrolprice.station_search_api.domain.type.Country;
import com.petrolprice.station_search_api.domain.type.Day;
import com.petrolprice.station_search_api.domain.type.ProductType;
import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.StationImportCompletedEvent;
import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto.AddressMessage;
import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto.FuelPriceMessage;
import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto.LocationMessage;
import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto.OpeningPeriodMessage;
import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto.StationSnapshotMessage;
import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto.StationSnapshotPayload;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Test-data builder for the station import workflow. Defaults are valid and every fixture is isolated. */
public final class StationSnapshotFixture {
    private UUID snapshotId = UUID.randomUUID();
    private UUID eventId = UUID.randomUUID();
    private String externalId = "station-" + UUID.randomUUID();
    private String brand = "REPSOL";
    private List<ProductPrice> prices = List.of(price(ProductType.DIESEL_A, "1.599"));

    private StationSnapshotFixture() {}

    public static StationSnapshotFixture aStationSnapshot() {
        return new StationSnapshotFixture();
    }

    public StationSnapshotFixture withSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
        return this;
    }

    public StationSnapshotFixture withEventId(UUID eventId) {
        this.eventId = eventId;
        return this;
    }

    public StationSnapshotFixture withExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public StationSnapshotFixture withBrand(String brand) {
        this.brand = brand;
        return this;
    }

    public StationSnapshotFixture withPrices(ProductPrice... prices) {
        this.prices = List.of(prices);
        return this;
    }

    public UUID snapshotId() {
        return snapshotId;
    }

    public UUID eventId() {
        return eventId;
    }

    public String externalId() {
        return externalId;
    }

    public ProcessStationSnapshotCommand processCommand() {
        return new ProcessStationSnapshotCommand(eventId, snapshotId, station());
    }

    public CompleteStationPublishingCommand completionCommand(int publishedStations) {
        return new CompleteStationPublishingCommand(snapshotId, publishedStations, Instant.now());
    }

    public StationSnapshotMessage message() {
        return new StationSnapshotMessage(
                snapshotId,
                eventId,
                "STATION_SNAPSHOT_CREATED",
                "1",
                Instant.now(),
                new StationSnapshotPayload(
                        externalId,
                        "ES",
                        brand,
                        brand,
                        List.of(new OpeningPeriodMessage(
                                "07:00:00", "22:00:00", List.of("MON", "TUE", "WED"))),
                        new AddressMessage("Test street", "28001", "Madrid", "Madrid", "Madrid"),
                        new LocationMessage(40.4168, -3.7038),
                        prices.stream()
                                .map(price -> new FuelPriceMessage(
                                        price.getProductType().name(), price.getPrice().doubleValue()))
                                .toList()),
                "integration-test");
    }

    public StationImportCompletedEvent completionEvent(int publishedStations) {
        return new StationImportCompletedEvent(snapshotId, publishedStations, Instant.now());
    }

    public static ProductPrice price(ProductType type, String value) {
        return ProductPrice.builder().productType(type).price(new BigDecimal(value)).build();
    }

    private Station station() {
        return Station.builder()
                .externalId(externalId)
                .country(Country.ES)
                .brand(brand)
                .normalizedBrand(brand)
                .openingPeriods(List.of(OpeningPeriod.builder()
                        .days(List.of(Day.MON, Day.TUE, Day.WED))
                        .open(LocalTime.of(7, 0))
                        .close(LocalTime.of(22, 0))
                        .build()))
                .address(Address.builder()
                        .street("Test street")
                        .postalCode("28001")
                        .locality("Madrid")
                        .municipality("Madrid")
                        .province("Madrid")
                        .build())
                .location(GeoLocation.builder()
                        .latitude(new BigDecimal("40.4168"))
                        .longitude(new BigDecimal("-3.7038"))
                        .build())
                .productPrices(prices)
                .build();
    }
}
