package com.petrolprice.station_search_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StationSearchApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StationSearchApiApplication.class, args);
    }
}
