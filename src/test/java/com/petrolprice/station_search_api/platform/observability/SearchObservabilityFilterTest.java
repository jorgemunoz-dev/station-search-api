package com.petrolprice.station_search_api.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.ServletException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SearchObservabilityFilterTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final Logger logger = (Logger) LoggerFactory.getLogger(SearchObservabilityFilter.class);
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
    void normalSuccessIsSilentAndArbitraryParameterNamesCannotCreateSeries() throws Exception {
        var request = new MockHttpServletRequest("GET", "/stations");
        request.addParameter("countryCode", "ES");
        request.addParameter("attacker-controlled-key", "secret");
        var response = new MockHttpServletResponse();
        filter(Duration.ofSeconds(10)).doFilter(request, response, (req, res) -> {});
        assertThat(events.list).isEmpty();
        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(registry.find("station.search.filter.usage")
                        .tag("filter", "attacker-controlled-key")
                        .counter())
                .isNull();
        assertThat(registry.get("station.search.queries")
                        .tag("outcome", "SUCCESS")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void allSlowQueriesAreCountedButOnlyOneWarningIsLoggedPerEndpoint() throws Exception {
        var filter = filter(Duration.ofNanos(1));
        for (int i = 0; i < 2; i++)
            filter.doFilter(
                    new MockHttpServletRequest("GET", "/stations"), new MockHttpServletResponse(), (req, res) -> {});
        assertThat(registry.get("station.search.slow").counter().count()).isEqualTo(2);
        assertThat(events.list).hasSize(1);
        assertThat(events.list.getFirst().getLevel()).isEqualTo(Level.WARN);
    }

    @Test
    void clientErrorsDoNotGenerateWarningsEvenWhenSlow() throws Exception {
        filter(Duration.ofNanos(1))
                .doFilter(
                        new MockHttpServletRequest("GET", "/stations"),
                        new MockHttpServletResponse(),
                        (req, res) -> ((MockHttpServletResponse) res).setStatus(400));
        assertThat(events.list).isEmpty();
        assertThat(registry.get("station.search.queries")
                        .tag("outcome", "CLIENT_ERROR")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void serverFailureProducesOneErrorAndNotAnAdditionalSlowWarning() throws Exception {
        filter(Duration.ofNanos(1))
                .doFilter(
                        new MockHttpServletRequest("GET", "/stations"),
                        new MockHttpServletResponse(),
                        (req, res) -> ((MockHttpServletResponse) res).setStatus(503));
        assertThat(events.list).hasSize(1);
        assertThat(events.list.getFirst().getLevel()).isEqualTo(Level.ERROR);
        assertThat(registry.get("station.search.queries")
                        .tag("outcome", "SERVER_ERROR")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void doesNotLogHandledExceptionsTwice() throws Exception {
        var request = new MockHttpServletRequest("GET", "/stations");
        request.setAttribute(SearchObservabilityFilter.FAILURE_LOGGED_ATTRIBUTE, true);
        filter(Duration.ofNanos(1))
                .doFilter(request, new MockHttpServletResponse(), (req, res) -> ((MockHttpServletResponse) res)
                        .setStatus(500));
        assertThat(events.list).isEmpty();
    }

    @Test
    void propagatedFailuresAreRecordedAndUnmatchedPathsRemainBounded() {
        var request = new MockHttpServletRequest("GET", "/locations/random-value");
        assertThatThrownBy(() -> filter(Duration.ofSeconds(10))
                        .doFilter(request, new MockHttpServletResponse(), (req, res) -> {
                            throw new ServletException("failure");
                        }))
                .isInstanceOf(ServletException.class);
        assertThat(registry.get("station.search.queries")
                        .tag("endpoint", "/locations/**")
                        .tag("outcome", "SERVER_ERROR")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(events.list).hasSize(1);
    }

    private SearchObservabilityFilter filter(Duration threshold) {
        return new SearchObservabilityFilter(
                new SearchObservabilityProperties(List.of("/stations", "/locations/**"), threshold), registry);
    }
}
