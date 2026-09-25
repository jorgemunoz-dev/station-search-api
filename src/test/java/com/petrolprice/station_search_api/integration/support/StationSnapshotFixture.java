package com.petrolprice.station_search_api.integration.support;

import com.petrolprice.station_search_api.station.domain.model.Address;
import com.petrolprice.station_search_api.station.domain.model.GeoLocation;
import com.petrolprice.station_search_api.station.domain.model.OpeningPeriod;
import com.petrolprice.station_search_api.station.domain.model.ProductPrice;
import com.petrolprice.station_search_api.station.domain.model.Station;
import com.petrolprice.station_search_api.station.domain.type.Country;
import com.petrolprice.station_search_api.station.domain.type.Day;
import com.petrolprice.station_search_api.station.domain.type.ProductType;
import com.petrolprice.station_search_api.station.ingestion.application.command.CompleteStationPublishingCommand;
import com.petrolprice.station_search_api.station.ingestion.application.command.ProcessStationSnapshotCommand;
import com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.StationImportCompletedEvent;
import com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto.AddressMessage;
import com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto.FuelPriceMessage;
import com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto.LocationMessage;
import com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto.OpeningPeriodMessage;
import com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto.StationSnapshotMessage;
import com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto.StationSnapshotPayload;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Test-data builder for the station import workflow. Defaults are valid and every fixture is isolated. */
public final class StationSnapshotFixture {
    private UUID snapshotId = UUID.randomUUID();
    private UUID eventId = UUID.randomUUID();
    private String externalId = "station-" + UUID.randomUUID();
    private String brand = "BRAND-" + UUID.randomUUID().toString().substring(0, 8);
    private BigDecimal latitude = randomCoordinate(37, 42);
    private BigDecimal longitude = randomCoordinate(-7, 2);
    private Address address = randomAddress();
    private List<ProductPrice> prices = List.of(price(ProductType.DIESEL_A, "1.599"));
    private List<OpeningPeriod> openingPeriods =
            List.of(openingPeriod(List.of(Day.MON, Day.TUE, Day.WED), LocalTime.of(7, 0), LocalTime.of(22, 0)));

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

    public StationSnapshotFixture withLocality(String locality) {
        this.address = Address.builder()
                .street(address.getStreet())
                .postalCode(address.getPostalCode())
                .locality(locality)
                .municipality(locality)
                .province(address.getProvince())
                .build();
        return this;
    }

    public StationSnapshotFixture withLocation(String latitude, String longitude) {
        this.latitude = new BigDecimal(latitude);
        this.longitude = new BigDecimal(longitude);
        return this;
    }

    public StationSnapshotFixture withLocation(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        return this;
    }

    public StationSnapshotFixture withOpeningPeriods(OpeningPeriod... openingPeriods) {
        this.openingPeriods = List.of(openingPeriods);
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

    public BigDecimal latitude() {
        return latitude;
    }

    public BigDecimal longitude() {
        return longitude;
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
                        openingPeriods.stream()
                                .map(period -> new OpeningPeriodMessage(
                                        period.getOpen().toString(),
                                        period.getClose().toString(),
                                        period.getDays().stream()
                                                .map(Enum::name)
                                                .toList()))
                                .toList(),
                        new AddressMessage(
                                address.getStreet(),
                                address.getPostalCode(),
                                address.getLocality(),
                                address.getMunicipality(),
                                address.getProvince()),
                        new LocationMessage(latitude.doubleValue(), longitude.doubleValue()),
                        prices.stream()
                                .map(price -> new FuelPriceMessage(
                                        price.getProductType().name(),
                                        price.getPrice().doubleValue()))
                                .toList()),
                "integration-test");
    }

    public StationImportCompletedEvent completionEvent(int publishedStations) {
        return new StationImportCompletedEvent(snapshotId, publishedStations, Instant.now());
    }

    public static ProductPrice price(ProductType type, String value) {
        return ProductPrice.builder()
                .productType(type)
                .price(new BigDecimal(value))
                .build();
    }

    public static OpeningPeriod openingPeriod(List<Day> days, LocalTime open, LocalTime close) {
        return OpeningPeriod.builder().days(days).open(open).close(close).build();
    }

    private Station station() {
        return Station.builder()
                .externalId(externalId)
                .country(Country.ES)
                .brand(brand)
                .normalizedBrand(brand)
                .openingPeriods(openingPeriods)
                .address(address)
                .location(GeoLocation.builder()
                        .latitude(latitude)
                        .longitude(longitude)
                        .build())
                .productPrices(prices)
                .build();
    }

    private static Address randomAddress() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return Address.builder()
                .street("Street " + suffix)
                .postalCode("%05d".formatted(ThreadLocalRandom.current().nextInt(100_000)))
                .locality("Locality " + suffix)
                .municipality("Municipality " + suffix)
                .province("Province " + suffix)
                .build();
    }

    private static BigDecimal randomCoordinate(double minimum, double maximum) {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(minimum, maximum));
    }
}
