package com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.dto;

import java.util.List;

public record OpeningPeriodMessage(String open, String close, List<String> days) {}
