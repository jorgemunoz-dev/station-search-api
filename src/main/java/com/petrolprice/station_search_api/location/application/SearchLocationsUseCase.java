package com.petrolprice.station_search_api.location.application;

import com.petrolprice.station_search_api.location.application.query.SearchLocationQuery;
import com.petrolprice.station_search_api.location.application.result.SearchLocationResult;
import java.util.List;

public interface SearchLocationsUseCase {
    List<SearchLocationResult> search(SearchLocationQuery query);
}
