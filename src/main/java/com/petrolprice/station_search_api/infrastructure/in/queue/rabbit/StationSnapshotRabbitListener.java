package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit;

import com.petrolprice.station_search_api.application.usecase.stationSnapshots.CompleteStationPublishingUseCase;
import com.petrolprice.station_search_api.application.usecase.stationSnapshots.ProcessStationSnapshotService;
import com.petrolprice.station_search_api.application.usecase.stationSnapshots.command.CompleteStationPublishingCommand;
import com.petrolprice.station_search_api.application.usecase.stationSnapshots.command.ProcessStationSnapshotCommand;
import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto.StationSnapshotMessage;
import com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.mapper.StationSnapshotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StationSnapshotRabbitListener {

    private final StationSnapshotMapper mapper;
    private final ProcessStationSnapshotService processStationSnapshotService;
    private final CompleteStationPublishingUseCase completeStationPublishingUseCase;

    @RabbitListener(queues = "station.snapshot.ingestion.queue")
    public void onSnapshotCreated(StationSnapshotMessage event) {
        ProcessStationSnapshotCommand command = mapper.toCommand(event);
        processStationSnapshotService.consume(command);
    }

    @RabbitListener(queues = "station.snapshot.completed.queue")
    public void onSnapshotCompleted(StationImportCompletedEvent event) {
        CompleteStationPublishingCommand command =
            new CompleteStationPublishingCommand(
                event.snapshotId(),
                event.publishedEvents(),
                event.completedAt()
            );

        completeStationPublishingUseCase.complete(command);
    }
}
