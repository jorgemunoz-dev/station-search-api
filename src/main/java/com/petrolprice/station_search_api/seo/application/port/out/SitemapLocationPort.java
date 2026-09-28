package com.petrolprice.station_search_api.seo.application.port.out;

import com.petrolprice.station_search_api.seo.application.SitemapLocation;
import java.util.List;

public interface SitemapLocationPort {
    List<SitemapLocation> findLocalitiesWithContent();
}
