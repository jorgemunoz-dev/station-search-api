package com.petrolprice.station_search_api.seo.application;

import java.time.Instant;

public record SitemapLocation(
        String countryCode,
        String adminArea1Name,
        String adminArea2Name,
        String normalizedLocalityName,
        Instant contentUpdatedAt) {}
