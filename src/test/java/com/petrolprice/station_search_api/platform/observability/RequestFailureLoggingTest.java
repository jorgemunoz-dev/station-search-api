package com.petrolprice.station_search_api.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.petrolprice.station_search_api.platform.rest.exception.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class RequestFailureLoggingTest {
    @Test
    void handledFailureHasCorrelationAndStackTraceWithoutDuplicateFilterError() throws Exception {
        var logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        var filterLogger = (Logger) LoggerFactory.getLogger(SearchObservabilityFilter.class);
        var events = new ListAppender<ILoggingEvent>();
        events.start();
        logger.addAppender(events);
        filterLogger.addAppender(events);
        var registry = new SimpleMeterRegistry();
        try (var trace = MDC.putCloseable("traceId", "test-trace")) {
            var filter = new SearchObservabilityFilter(
                    new SearchObservabilityProperties(List.of("/stations"), Duration.ofSeconds(2)), registry);
            var mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .addFilters(filter)
                    .build();
            mvc.perform(get("/stations").header("X-Request-Id", "request-123"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(header().string("X-Request-Id", "request-123"))
                    .andExpect(jsonPath("$.requestId").value("request-123"))
                    .andExpect(jsonPath("$.traceId").value("test-trace"));
            assertThat(events.list).hasSize(1);
            var event = events.list.getFirst();
            assertThat(event.getThrowableProxy()).isNotNull();
            assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "request-123");
            assertThat(event.getKeyValuePairs()).anySatisfy(pair -> {
                assertThat(pair.key).isEqualTo("endpoint");
                assertThat(pair.value).isEqualTo("/stations");
            });
            assertThat(registry.get("station.search.queries")
                            .tag("outcome", "SERVER_ERROR")
                            .counter()
                            .count())
                    .isEqualTo(1);
        } finally {
            registry.close();
            logger.detachAppender(events);
            filterLogger.detachAppender(events);
            events.stop();
        }
    }

    @RestController
    static class FailingController {
        @GetMapping("/stations")
        public String fail() {
            throw new IllegalStateException("test database failure");
        }
    }
}
