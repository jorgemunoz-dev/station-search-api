package com.petrolprice.station_search_api.infrastructure.in.rest.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SearchObservabilityFilterTest {

    private SimpleMeterRegistry meterRegistry;
    private SearchObservabilityFilter filter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        filter = new SearchObservabilityFilter(
                meterRegistry,
                "/api/**",
                "searchMode,page,size,lat,lng,radiusMeters,productType,sortBy,query,countryCode,limit");
    }

    @Test
    void recordsQueryDurationOutcomeAndAllowListedFilters() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/api/v1/stations");
        request.addParameter("productType", "DIESEL");
        request.addParameter("radiusMeters", "5000");
        request.addParameter("unknown", "must-not-be-tagged");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(meterRegistry
                        .get("station.search.queries")
                        .tag("endpoint", "/api/**")
                        .tag("outcome", "SUCCESS")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.get("station.search.duration").timer().count()).isEqualTo(1);
        assertThat(meterRegistry
                        .get("station.search.filter.usage")
                        .tag("filter", "productType")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry
                        .get("station.search.filter.usage")
                        .tag("filter", "radiusMeters")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry
                        .find("station.search.filter.usage")
                        .tag("filter", "unknown")
                        .counter())
                .isNull();
        assertThat(meterRegistry.getMeters().stream()
                        .flatMap(meter -> meter.getId().getTags().stream())
                        .map(tag -> tag.getValue()))
                .doesNotContain("DIESEL", "5000", "must-not-be-tagged");
        assertThat(response.getHeader(SearchObservabilityFilter.REQUEST_ID_HEADER)).isNotBlank();
    }

    @Test
    void ignoresRequestsOutsideConfiguredApiPath() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/actuator/health");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(meterRegistry.find("station.search.queries").counter()).isNull();
    }

    @Test
    void preservesValidIncomingRequestId() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/api/v1/stations");
        request.addHeader(SearchObservabilityFilter.REQUEST_ID_HEADER, "frontend-123");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(SearchObservabilityFilter.REQUEST_ID_HEADER))
                .isEqualTo("frontend-123");
    }

    @Test
    void replacesUnsafeIncomingRequestId() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/api/v1/stations");
        request.addHeader(SearchObservabilityFilter.REQUEST_ID_HEADER, "unsafe value");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(SearchObservabilityFilter.REQUEST_ID_HEADER))
                .isNotEqualTo("unsafe value")
                .matches("[0-9a-f-]{36}");
    }
}
