package com.petrolprice.station_search_api.platform.observability;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.observability.search")
public record SearchObservabilityProperties(List<String> paths, Duration slowQueryThreshold) {
    private static final Duration DEFAULT_SLOW_QUERY_THRESHOLD = Duration.ofSeconds(2);

    public SearchObservabilityProperties {
        paths = paths == null ? List.of() : List.copyOf(paths);
        slowQueryThreshold = slowQueryThreshold == null ? DEFAULT_SLOW_QUERY_THRESHOLD : slowQueryThreshold;

        if (slowQueryThreshold.isNegative() || slowQueryThreshold.isZero()) {
            throw new IllegalArgumentException("slowQueryThreshold must be greater than zero");
        }
    }
}
