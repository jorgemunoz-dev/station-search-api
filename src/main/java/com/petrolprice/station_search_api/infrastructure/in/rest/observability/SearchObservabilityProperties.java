package com.petrolprice.station_search_api.infrastructure.in.rest.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.observability.search")
public record SearchObservabilityProperties(
    List<String> paths
) {}
