package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit;

import com.petrolprice.station_search_api.station.ingestion.application.CompleteStationPublishingUseCase;
import com.petrolprice.station_search_api.station.ingestion.application.ProcessStationSnapshotService;
import com.petrolprice.station_search_api.station.ingestion.application.command.CompleteStationPublishingCommand;
import com.petrolprice.station_search_api.station.ingestion.application.command.ProcessStationSnapshotCommand;
import com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto.StationSnapshotMessage;
import com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.mapper.StationSnapshotMapper;
import java.util.List;
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

    @RabbitListener(queues = "station.snapshot.ingestion.queue", containerFactory = "snapshotRabbitListenerContainerFactory")
    public void onSnapshotCreated(List<StationSnapshotMessage> events) {
        List<ProcessStationSnapshotCommand> commands = events.stream().map(mapper::toCommand).toList();
        processStationSnapshotService.consume(commands);
    }

    @RabbitListener(queues = "station.snapshot.completed.queue", containerFactory = "completionRabbitListenerContainerFactory")
    public void onSnapshotCompleted(StationImportCompletedEvent event) {
        CompleteStationPublishingCommand command =
                new CompleteStationPublishingCommand(event.snapshotId(), event.publishedEvents(), event.completedAt());

        completeStationPublishingUseCase.complete(command);
    }
}
