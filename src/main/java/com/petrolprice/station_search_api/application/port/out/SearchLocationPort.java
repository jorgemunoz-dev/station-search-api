package com.petrolprice.station_search_api.application.port.out;

import com.petrolprice.station_search_api.application.usecase.searchlocation.query.SearchLocationQuery;
import com.petrolprice.station_search_api.application.usecase.searchlocation.result.SearchLocationResult;
import java.util.List;

public interface SearchLocationPort {
    List<SearchLocationResult> search(SearchLocationQuery query);
}
