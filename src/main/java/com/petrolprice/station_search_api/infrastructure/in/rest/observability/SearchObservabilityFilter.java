package com.petrolprice.station_search_api.infrastructure.in.rest.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Configuration
@EnableConfigurationProperties(SearchObservabilityProperties.class)
public class SearchObservabilityFilter extends OncePerRequestFilter {
    static final String REQUEST_ID_HEADER = "X-Request-Id";
    private final SearchObservabilityProperties properties;

    private static final Logger log = LoggerFactory.getLogger(SearchObservabilityFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final MeterRegistry meterRegistry;

    public SearchObservabilityFilter(
        SearchObservabilityProperties properties,
        MeterRegistry meterRegistry
    ) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String requestId = requestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", requestId)) {
            if (!isObservedQuery(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            long startedAt = System.nanoTime();
            Throwable failure = null;
            try {
                filterChain.doFilter(request, response);
            } catch (IOException | ServletException | RuntimeException exception) {
                failure = exception;
                throw exception;
            } finally {
                record(request, response, startedAt, failure);
            }
        }
    }

    private boolean isObservedQuery(HttpServletRequest request) {
        return "GET".equals(request.getMethod())
            && properties.paths().stream()
            .anyMatch(path -> PATH_MATCHER.match(path, request.getRequestURI()));
    }

    private void record(
        HttpServletRequest request,
        HttpServletResponse response,
        long startedAt,
        Throwable failure) {
        long durationNanos = System.nanoTime() - startedAt;
        String endpoint = endpoint(request);
        String outcome = outcome(response.getStatus(), failure);
        var filterNames = Collections.list(request.getParameterNames())
            .stream()
            .sorted()
            .toList();

        Counter.builder("station.search.queries")
            .description("Number of station API queries")
            .tag("endpoint", endpoint)
            .tag("outcome", outcome)
            .register(meterRegistry)
            .increment();

        Timer.builder("station.search.duration")
            .description("Station API query duration")
            .tag("endpoint", endpoint)
            .tag("outcome", outcome)
            .publishPercentileHistogram()
            .register(meterRegistry)
            .record(durationNanos, TimeUnit.NANOSECONDS);

        filterNames.forEach(filter -> Counter.builder("station.search.filter.usage")
            .description("Number of API queries using a supported filter")
            .tag("filter", filter)
            .register(meterRegistry)
            .increment());

        log.atInfo()
            .addKeyValue("event", "station_search_completed")
            .addKeyValue("method", request.getMethod())
            .addKeyValue("endpoint", endpoint)
            .addKeyValue("status", response.getStatus())
            .addKeyValue("outcome", outcome)
            .addKeyValue("durationMs", TimeUnit.NANOSECONDS.toMillis(durationNanos))
            .addKeyValue("filters", filterNames)
            .log("Station API query completed");
    }

    private String endpoint(HttpServletRequest request) {
        Object pattern =
            request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        return pattern == null
            ? request.getRequestURI()
            : pattern.toString();
    }

    private static String outcome(int status, Throwable failure) {
        if (failure != null || status >= 500) {
            return "SERVER_ERROR";
        }
        if (status >= 400) {
            return "CLIENT_ERROR";
        }
        return "SUCCESS";
    }

    private static String requestId(HttpServletRequest request) {
        String supplied = request.getHeader(REQUEST_ID_HEADER);
        if (supplied != null && supplied.matches("[A-Za-z0-9._-]{1,100}")) {
            return supplied;
        }
        return UUID.randomUUID().toString();
    }
}
