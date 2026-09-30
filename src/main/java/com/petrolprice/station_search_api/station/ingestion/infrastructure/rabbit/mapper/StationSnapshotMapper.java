package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.mapper;

import com.petrolprice.station_search_api.station.domain.model.ProductPrice;
import com.petrolprice.station_search_api.station.domain.model.Station;
import com.petrolprice.station_search_api.station.domain.type.ProductType;
import com.petrolprice.station_search_api.station.ingestion.application.command.ProcessStationSnapshotCommand;
import com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto.FuelPriceMessage;
import com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto.StationSnapshotMessage;
import com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto.StationSnapshotPayload;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StationSnapshotMapper {

    @Mapping(target = "snapshotId", source = "batchId")
    @Mapping(target = "observedAt", source = "occurredAt")
    @Mapping(target = "station", source = "payload")
    ProcessStationSnapshotCommand toCommand(StationSnapshotMessage message);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productPrices", source = "fuelPrices")
    @Mapping(target = "openingPeriods", source = "schedule")
    Station toModel(StationSnapshotPayload message);

    @Mapping(target = "productType", source = "fuelType")
    ProductPrice toModel(FuelPriceMessage message);

    default ProductType map(String productType) {
        if (productType == null) {
            throw new IllegalArgumentException("Product type cannot be null in station snapshot");
        }

        try {
            return ProductType.valueOf(productType);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown product type received: " + productType, ex);
        }
    }
}
