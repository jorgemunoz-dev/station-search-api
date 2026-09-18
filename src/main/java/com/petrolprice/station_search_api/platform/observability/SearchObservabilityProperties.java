package com.petrolprice.station_search_api.platform.observability;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.observability.search")
public record SearchObservabilityProperties(List<String> paths) {}
