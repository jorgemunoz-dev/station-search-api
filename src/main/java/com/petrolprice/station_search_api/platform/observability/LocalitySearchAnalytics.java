package com.petrolprice.station_search_api.platform.observability;

import com.petrolprice.station_search_api.station.search.application.query.FindStationsQuery;
import com.petrolprice.station_search_api.station.search.application.searcharea.LocalitySearchArea;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Business events, separate from technical warnings/errors. Never metric labels per town. */
@Component
public class LocalitySearchAnalytics {
    private static final Logger log = LoggerFactory.getLogger(LocalitySearchAnalytics.class);
    private final MeterRegistry registry;

    public LocalitySearchAnalytics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(FindStationsQuery query, int resultCount) {
        if (query.pageRequest().page() != 0 || !(query.searchArea() instanceof LocalitySearchArea area)) return;
        // These are normalized administrative names, not autocomplete text,
        // coordinates, IPs or URLs. Bound the event payload even for API callers.
        if (!safe(area.normalizedAdminArea1())
                || !safe(area.normalizedAdminArea2())
                || !safe(area.normalizedAdminArea3())) return;

        Counter.builder("station.search.locality.consultations")
                .description("Successful first-page locality queries, not unique visitors")
                .tag("result", resultCount == 0 ? "EMPTY" : "NON_EMPTY")
                .register(registry)
                .increment();

        log.atInfo()
                .addKeyValue("event", "locality_consulted")
                .addKeyValue("country", area.countryCode())
                .addKeyValue("region", area.normalizedAdminArea1())
                .addKeyValue("province", area.normalizedAdminArea2())
                .addKeyValue("locality", area.normalizedAdminArea3())
                .addKeyValue(
                        "fuel",
                        query.productType() == null
                                ? "ALL"
                                : query.productType().name())
                .addKeyValue("resultCount", resultCount)
                .log("Locality consulted");
    }

    private static boolean safe(String name) {
        return name != null && name.length() <= 100 && name.matches("[\\p{L}\\p{N} ]+");
    }
}
