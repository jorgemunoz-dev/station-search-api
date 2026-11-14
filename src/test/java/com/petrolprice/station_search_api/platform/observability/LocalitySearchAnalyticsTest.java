package com.petrolprice.station_search_api.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.petrolprice.station_search_api.station.domain.type.ProductType;
import com.petrolprice.station_search_api.station.search.application.query.FindStationsPageRequest;
import com.petrolprice.station_search_api.station.search.application.query.FindStationsQuery;
import com.petrolprice.station_search_api.station.search.application.query.FindStationsSort;
import com.petrolprice.station_search_api.station.search.application.searcharea.LocalitySearchArea;
import com.petrolprice.station_search_api.station.search.application.searcharea.RadiusSearchArea;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class LocalitySearchAnalyticsTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final LocalitySearchAnalytics analytics = new LocalitySearchAnalytics(registry);
    private final Logger logger = (Logger) LoggerFactory.getLogger(LocalitySearchAnalytics.class);
    private final ListAppender<ILoggingEvent> events = new ListAppender<>();

    @BeforeEach
    void attach() {
        events.start();
        logger.addAppender(events);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(events);
        events.stop();
        registry.close();
    }

    @Test
    void countsOneBusinessQueryWithoutAddingLocalitiesToMetricLabels() {
        analytics.record(query(0, "Ardales"), 12);
        assertThat(registry.get("station.search.locality.consultations")
                        .tag("result", "NON_EMPTY")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(events.list).hasSize(1);
        assertThat(events.list.getFirst().getKeyValuePairs()).anySatisfy(pair -> {
            assertThat(pair.key).isEqualTo("locality");
            assertThat(pair.value).isEqualTo("ardales");
        });
        assertThat(registry.getMeters().getFirst().getId().getTags())
                .extracting(tag -> tag.getKey())
                .containsExactly("result");
    }

    @Test
    void excludesPaginationButRetainsEmptyLocalityQueries() {
        analytics.record(query(1, "Ardales"), 12);
        assertThat(events.list).isEmpty();
        analytics.record(query(0, "Ardales"), 0);
        assertThat(events.list).hasSize(1);
        assertThat(registry.get("station.search.locality.consultations")
                        .tag("result", "EMPTY")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void rejectsOversizedAnalyticsPayloads() {
        analytics.record(query(0, "x".repeat(101)), 0);
        assertThat(events.list).isEmpty();
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    void mapViewportAndRadiusQueriesDoNotBecomeLocalityConsultations() {
        var query = new FindStationsQuery(
                new RadiusSearchArea(BigDecimal.valueOf(36), BigDecimal.valueOf(-4), 1000),
                ProductType.DIESEL_A,
                FindStationsSort.PRICE,
                new FindStationsPageRequest(0, 10));
        analytics.record(query, 12);
        assertThat(events.list).isEmpty();
        assertThat(registry.getMeters()).isEmpty();
    }

    private FindStationsQuery query(int page, String town) {
        return new FindStationsQuery(
                new LocalitySearchArea("ES", "Andalucía", "Málaga", town),
                ProductType.DIESEL_A,
                FindStationsSort.PRICE,
                new FindStationsPageRequest(page, 10));
    }
}
