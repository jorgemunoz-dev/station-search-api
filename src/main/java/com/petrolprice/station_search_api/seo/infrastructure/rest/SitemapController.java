package com.petrolprice.station_search_api.seo.infrastructure.rest;

import com.petrolprice.station_search_api.seo.application.SitemapService;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seo")
public class SitemapController {
    private static final MediaType XML_UTF8 = MediaType.parseMediaType("application/xml; charset=utf-8");
    private final SitemapService sitemapService;
    private final Duration cacheTtl;

    public SitemapController(
            SitemapService sitemapService,
            @Value("${app.seo.sitemap.cache-ttl:PT1H}") Duration cacheTtl) {
        this.sitemapService = sitemapService;
        this.cacheTtl = cacheTtl;
    }

    @GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=UTF-8")
    public ResponseEntity<String> sitemap() {
        return response(sitemapService.sitemap());
    }

    @GetMapping(value = "/sitemap-{page}.xml", produces = "application/xml;charset=UTF-8")
    public ResponseEntity<String> sitemapPage(@PathVariable int page) {
        String xml = sitemapService.sitemapPage(page);
        return xml == null ? ResponseEntity.notFound().build() : response(xml);
    }

    private ResponseEntity<String> response(String xml) {
        return ResponseEntity.ok()
                .contentType(XML_UTF8)
                .cacheControl(CacheControl.maxAge(cacheTtl).cachePublic())
                .body(xml);
    }
}
