package com.petrolprice.station_search_api.seo.infrastructure.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petrolprice.station_search_api.seo.application.SitemapService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SitemapControllerTest {
    @Test
    void returnsXmlWithCacheHeaders() throws Exception {
        SitemapService service = mock(SitemapService.class);
        when(service.sitemap()).thenReturn("<?xml version=\"1.0\"?><urlset/>");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new SitemapController(service, Duration.ofHours(1))).build();

        mvc.perform(get("/seo/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/xml;charset=utf-8"))
                .andExpect(header().string("Cache-Control", "max-age=3600, public"))
                .andExpect(content().string("<?xml version=\"1.0\"?><urlset/>"));
    }

    @Test
    void returnsNotFoundForANonexistentPage() throws Exception {
        SitemapService service = mock(SitemapService.class);
        when(service.sitemapPage(3)).thenReturn(null);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new SitemapController(service, Duration.ofHours(1))).build();

        mvc.perform(get("/seo/sitemap-3.xml")).andExpect(status().isNotFound());
    }
}
