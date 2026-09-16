package com.petrolprice.station_search_api.station.ingestion.application;

import com.petrolprice.station_search_api.station.ingestion.application.command.CompleteStationPublishingCommand;

public interface CompleteStationPublishingUseCase {
    void complete(CompleteStationPublishingCommand command);
}
