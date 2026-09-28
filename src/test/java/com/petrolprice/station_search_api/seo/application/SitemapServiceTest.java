package com.petrolprice.station_search_api.seo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petrolprice.station_search_api.seo.application.port.out.SitemapLocationPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SitemapServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-28T10:15:30Z");

    @Test
    void normalizesSegmentsAndIncludesStaticUrls() {
        SitemapLocationPort port = mock(SitemapLocationPort.class);
        when(port.findLocalitiesWithContent())
                .thenReturn(List.of(new SitemapLocation("ES", "Comunitat Valènciana", "Alacant/Alicante",
                        "  Sant Joan d'Alacant  ", NOW)));

        String xml = service(port).sitemap();

        assertThat(xml)
                .contains("<loc>https://www.gasoamigos.es/</loc>")
                .contains("<loc>https://www.gasoamigos.es/gasolineras</loc>")
                .contains("<loc>https://www.gasoamigos.es/mapa</loc>")
                .contains("https://www.gasoamigos.es/gasolineras/es/comunitat-valenciana/alacant-alicante/sant-joan-d-alacant");
    }

    @Test
    void deduplicatesByTheRequiredGeographicalKeyAndKeepsLatestContentDate() {
        SitemapLocationPort port = mock(SitemapLocationPort.class);
        when(port.findLocalitiesWithContent()).thenReturn(List.of(
                new SitemapLocation("ES", "Madrid", "Madrid", "madrid", NOW.minusSeconds(60)),
                new SitemapLocation("ES", "Madrid", "Madrid", "madrid", NOW)));

        String xml = service(port).sitemap();

        assertThat(occurrences(xml, "/es/madrid/madrid/madrid</loc>")).isEqualTo(1);
        assertThat(xml).contains("<lastmod>2026-09-28T10:15:30Z</lastmod>");
    }

    @Test
    void escapesXmlAndCachesTheGeneratedDocument() {
        SitemapLocationPort port = mock(SitemapLocationPort.class);
        when(port.findLocalitiesWithContent()).thenReturn(List.of());
        SitemapService service = service(port);

        assertThat(SitemapService.xmlEscape("a&<b>\"'"))
                .isEqualTo("a&amp;&lt;b&gt;&quot;&apos;");
        assertThat(service.sitemap()).isEqualTo(service.sitemap());
        verify(port).findLocalitiesWithContent();
    }

    @Test
    void doesNotInventLastmodForStaticPages() {
        SitemapLocationPort port = mock(SitemapLocationPort.class);
        when(port.findLocalitiesWithContent()).thenReturn(List.of());

        assertThat(service(port).sitemap()).doesNotContain("<lastmod>");
    }

    @Test
    void createsAnIndexAndPagesWhenTheUrlLimitIsExceeded() {
        SitemapLocationPort port = mock(SitemapLocationPort.class);
        List<SitemapLocation> localities = IntStream.range(0, SitemapService.MAX_URLS - 2)
                .mapToObj(number -> new SitemapLocation("ES", "Area", "Province", "town-" + number, NOW))
                .toList();
        when(port.findLocalitiesWithContent()).thenReturn(localities);
        SitemapService service = service(port);

        assertThat(service.sitemap())
                .contains("<sitemapindex")
                .contains("https://www.gasoamigos.es/sitemap-1.xml")
                .contains("https://www.gasoamigos.es/sitemap-2.xml");
        assertThat(service.sitemapPage(1)).contains("<urlset");
        assertThat(service.sitemapPage(2)).contains("<urlset");
        assertThat(service.sitemapPage(3)).isNull();
    }

    private static SitemapService service(SitemapLocationPort port) {
        return new SitemapService(port, Duration.ofHours(1), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static int occurrences(String value, String fragment) {
        return (value.length() - value.replace(fragment, "").length()) / fragment.length();
    }
}
