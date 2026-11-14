package com.petrolprice.station_search_api.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petrolprice.station_search_api.station.ingestion.infrastructure.rabbit.RabbitMessageRecoverer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PrometheusMetricsContractTest {
    @Test
    void exportsTheNamesUsedByDashboardsAndAlerts() throws Exception {
        var registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        try {
            var filter = new SearchObservabilityFilter(
                    new SearchObservabilityProperties(List.of("/stations"), Duration.ofSeconds(10)), registry);
            filter.doFilter(new MockHttpServletRequest("GET", "/stations"),
                    new MockHttpServletResponse(), (req, res) -> {});
            var recoverer = new RabbitMessageRecoverer(registry);
            assertThatThrownBy(() -> recoverer.recover(new Message(new byte[0], new MessageProperties()),
                    new IllegalStateException("test failure")))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);
            assertThat(registry.scrape()).contains("station_search_queries_total",
                    "station_search_duration_seconds_bucket", "station_rabbit_retries_exhausted_total",
                    "station_rabbit_last_failure_timestamp_seconds");
        } finally {
            registry.close();
        }
    }
}
