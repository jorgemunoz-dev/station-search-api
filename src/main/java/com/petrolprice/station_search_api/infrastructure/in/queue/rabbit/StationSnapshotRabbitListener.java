package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit;

import com.petrolprice.station_search_api.application.usecase.ConsumeStationSnapshotUseCase;
import com.petrolprice.station_search_api.domain.model.Station;
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
    private final ConsumeStationSnapshotUseCase consumeStationSnapshotUseCase;

    @RabbitListener(queues = "station.snapshot.ingestion.queue")
    public void onSnapshotCreated(StationSnapshotMessage event) {
        Station station = mapper.toModel(event);
        consumeStationSnapshotUseCase.consume(station);
    }
}
