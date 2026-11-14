package com.petrolprice.station_search_api.platform.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Configuration
@EnableConfigurationProperties(SearchObservabilityProperties.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SearchObservabilityFilter extends OncePerRequestFilter {
    public static final String FAILURE_LOGGED_ATTRIBUTE = SearchObservabilityFilter.class.getName() + ".failureLogged";
    private static final Set<String> SUPPORTED_FILTERS = Set.of(
            "searchMode",
            "page",
            "size",
            "countryCode",
            "adminArea1",
            "adminArea2",
            "adminArea3",
            "lat",
            "lng",
            "radiusMeters",
            "north",
            "south",
            "east",
            "west",
            "productType",
            "sortBy",
            "query",
            "limit",
            "from",
            "to",
            "days",
            "stationId",
            "referencePrice",
            "tankLiters");
    private static final long SLOW_LOG_INTERVAL_NANOS = Duration.ofMinutes(1).toNanos();
    private final ConcurrentMap<String, Long> lastSlowWarnings = new ConcurrentHashMap<>();

    static final String REQUEST_ID_HEADER = "X-Request-Id";
    private final SearchObservabilityProperties properties;

    private static final Logger log = LoggerFactory.getLogger(SearchObservabilityFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final MeterRegistry meterRegistry;

    public SearchObservabilityFilter(SearchObservabilityProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
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
                && properties.paths().stream().anyMatch(path -> PATH_MATCHER.match(path, request.getRequestURI()));
    }

    private void record(HttpServletRequest request, HttpServletResponse response, long startedAt, Throwable failure) {
        long durationNanos = System.nanoTime() - startedAt;
        String endpoint = endpoint(request);
        String outcome = outcome(response.getStatus(), failure);
        var filterNames = Collections.list(request.getParameterNames()).stream()
                .filter(SUPPORTED_FILTERS::contains)
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
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);

        filterNames.forEach(filter -> Counter.builder("station.search.filter.usage")
                .description("Number of API queries using a supported filter")
                .tag("filter", filter)
                .register(meterRegistry)
                .increment());

        long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);
        boolean slow = durationNanos >= properties.slowQueryThreshold().toNanos();
        if (slow) {
            Counter.builder("station.search.slow")
                    .description("Number of queries exceeding the latency threshold")
                    .tag("endpoint", endpoint)
                    .tag("outcome", outcome)
                    .register(meterRegistry)
                    .increment();
        }
        if ("SERVER_ERROR".equals(outcome)) {
            if (!Boolean.TRUE.equals(request.getAttribute(FAILURE_LOGGED_ATTRIBUTE))) {
                log.atError()
                        .setCause(failure)
                        .addKeyValue("event", "station_request_failed")
                        .addKeyValue("endpoint", endpoint)
                        .addKeyValue("status", failure == null ? response.getStatus() : 500)
                        .addKeyValue("durationMs", durationMs)
                        .log("Station API request failed");
            }
            return;
        }
        // Metrics include every slow query, but warnings are limited to one per
        // endpoint/minute. Normal 4xx responses are not operational incidents.
        if (!slow || response.getStatus() >= 400 || !shouldWarn(endpoint)) return;

        log.atWarn()
                .addKeyValue("event", "station_search_slow")
                .addKeyValue("method", request.getMethod())
                .addKeyValue("endpoint", endpoint)
                .addKeyValue("status", response.getStatus())
                .addKeyValue("outcome", outcome)
                .addKeyValue("durationMs", durationMs)
                .addKeyValue("thresholdMs", properties.slowQueryThreshold().toMillis())
                .addKeyValue("filters", filterNames)
                .log("Slow station API query");
    }

    private String endpoint(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        return pattern == null
                ? properties.paths().stream()
                        .filter(path -> PATH_MATCHER.match(path, request.getRequestURI()))
                        .findFirst()
                        .orElse("unmatched")
                : pattern.toString();
    }

    private boolean shouldWarn(String endpoint) {
        long now = System.nanoTime();
        Long previous = lastSlowWarnings.putIfAbsent(endpoint, now);
        return previous == null
                || (now - previous >= SLOW_LOG_INTERVAL_NANOS && lastSlowWarnings.replace(endpoint, previous, now));
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
