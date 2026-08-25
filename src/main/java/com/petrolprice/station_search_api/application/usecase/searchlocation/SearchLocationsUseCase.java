package com.petrolprice.station_search_api.application.usecase.searchlocation;

import com.petrolprice.station_search_api.application.usecase.searchlocation.query.SearchLocationQuery;
import com.petrolprice.station_search_api.application.usecase.searchlocation.result.SearchLocationResult;
import java.util.List;

public interface SearchLocationsUseCase {
    List<SearchLocationResult> search(SearchLocationQuery query);
}
