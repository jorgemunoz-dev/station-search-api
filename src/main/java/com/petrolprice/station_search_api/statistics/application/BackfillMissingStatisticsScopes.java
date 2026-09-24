package com.petrolprice.station_search_api.statistics.application;

import com.petrolprice.station_search_api.statistics.application.port.out.StatisticsCalculationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackfillMissingStatisticsScopes implements ApplicationRunner {
    private final StatisticsCalculationRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int insertedRows = repository.backfillMissingScopes();
        if (insertedRows > 0) {
            log.info("Backfilled {} missing locality and administrative statistics rows", insertedRows);
        }
    }
}
