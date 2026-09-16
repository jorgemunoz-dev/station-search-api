package com.petrolprice.station_search_api.application.usecase.stationSnapshots;

import com.petrolprice.station_search_api.application.usecase.stationSnapshots.command.CompleteStationPublishingCommand;

public interface CompleteStationPublishingUseCase {
    void complete(CompleteStationPublishingCommand command);
}
