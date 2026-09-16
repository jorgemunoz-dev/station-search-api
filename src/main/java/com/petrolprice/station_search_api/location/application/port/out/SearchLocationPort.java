package com.petrolprice.station_search_api.location.application.port.out;

import com.petrolprice.station_search_api.location.application.query.SearchLocationQuery;
import com.petrolprice.station_search_api.location.application.result.SearchLocationResult;
import java.util.List;

public interface SearchLocationPort {
    List<SearchLocationResult> search(SearchLocationQuery query);
}
