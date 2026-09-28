package com.petrolprice.station_search_api.seo.application;

import com.petrolprice.station_search_api.seo.application.port.out.SitemapLocationPort;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SitemapService {
    static final int MAX_URLS = 50_000;
    private static final String SITE = "https://www.gasoamigos.es";
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private final SitemapLocationPort locationPort;
    private final Duration cacheTtl;
    private final Clock clock;
    private volatile CachedSitemaps cache;

    public SitemapService(
            SitemapLocationPort locationPort,
            @Value("${app.seo.sitemap.cache-ttl:PT1H}") Duration cacheTtl) {
        this(locationPort, cacheTtl, Clock.systemUTC());
    }

    SitemapService(SitemapLocationPort locationPort, Duration cacheTtl, Clock clock) {
        this.locationPort = locationPort;
        this.cacheTtl = cacheTtl;
        this.clock = clock;
    }

    public String sitemap() {
        return current().root();
    }

    public String sitemapPage(int page) {
        List<String> pages = current().pages();
        if (pages.size() <= 1 || page < 1 || page > pages.size()) {
            return null;
        }
        return pages.get(page - 1);
    }

    private CachedSitemaps current() {
        CachedSitemaps current = cache;
        Instant now = clock.instant();
        if (current == null || !now.isBefore(current.expiresAt())) {
            synchronized (this) {
                current = cache;
                if (current == null || !now.isBefore(current.expiresAt())) {
                    current = generate(now);
                    cache = current;
                }
            }
        }
        return current;
    }

    private CachedSitemaps generate(Instant now) {
        Map<LocalityKey, SitemapLocation> unique = new LinkedHashMap<>();
        for (SitemapLocation locality : locationPort.findLocalitiesWithContent()) {
            LocalityKey key = new LocalityKey(
                    locality.countryCode(),
                    locality.adminArea1Name(),
                    locality.adminArea2Name(),
                    locality.normalizedLocalityName());
            unique.merge(key, locality, SitemapService::latest);
        }

        List<SitemapUrl> urls = new ArrayList<>();
        urls.add(new SitemapUrl(SITE + "/", null));
        urls.add(new SitemapUrl(SITE + "/gasolineras", null));
        urls.add(new SitemapUrl(SITE + "/mapa", null));
        unique.values().stream()
                .map(this::toUrl)
                .sorted((left, right) -> left.location().compareTo(right.location()))
                .forEach(urls::add);

        List<String> pages = new ArrayList<>();
        for (int start = 0; start < urls.size(); start += MAX_URLS) {
            pages.add(urlSet(urls.subList(start, Math.min(start + MAX_URLS, urls.size()))));
        }
        String root = pages.size() == 1 ? pages.getFirst() : sitemapIndex(pages.size());
        return new CachedSitemaps(root, List.copyOf(pages), now.plus(cacheTtl));
    }

    private SitemapUrl toUrl(SitemapLocation locality) {
        String path = "/gasolineras/" + slug(locality.countryCode()) + "/" + slug(locality.adminArea1Name())
                + "/" + slug(locality.adminArea2Name()) + "/" + slug(locality.normalizedLocalityName());
        return new SitemapUrl(SITE + path, locality.contentUpdatedAt());
    }

    static String slug(String value) {
        String decomposed = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        String ascii = MARKS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT);
        return NON_ALPHANUMERIC.matcher(ascii).replaceAll("-").replaceAll("^-|-$", "");
    }

    static String xmlEscape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static SitemapLocation latest(SitemapLocation left, SitemapLocation right) {
        if (left.contentUpdatedAt() == null) return right;
        if (right.contentUpdatedAt() == null) return left;
        return left.contentUpdatedAt().isAfter(right.contentUpdatedAt()) ? left : right;
    }

    private static String urlSet(List<SitemapUrl> urls) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (SitemapUrl url : urls) {
            xml.append("  <url><loc>").append(xmlEscape(url.location())).append("</loc>");
            if (url.lastModified() != null) {
                xml.append("<lastmod>").append(DateTimeFormatter.ISO_INSTANT.format(url.lastModified()))
                        .append("</lastmod>");
            }
            xml.append("</url>\n");
        }
        return xml.append("</urlset>\n").toString();
    }

    private static String sitemapIndex(int pages) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (int page = 1; page <= pages; page++) {
            xml.append("  <sitemap><loc>").append(SITE).append("/sitemap-").append(page)
                    .append(".xml</loc></sitemap>\n");
        }
        return xml.append("</sitemapindex>\n").toString();
    }

    private record SitemapUrl(String location, Instant lastModified) {}
    private record LocalityKey(String country, String adminArea1, String adminArea2, String locality) {}
    private record CachedSitemaps(String root, List<String> pages, Instant expiresAt) {}
}
