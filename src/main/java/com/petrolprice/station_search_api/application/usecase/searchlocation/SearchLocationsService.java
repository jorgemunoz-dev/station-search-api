package com.petrolprice.station_search_api.application.usecase.searchlocation;

import com.petrolprice.station_search_api.application.port.out.SearchLocationPort;
import com.petrolprice.station_search_api.application.usecase.searchlocation.query.SearchLocationQuery;
import com.petrolprice.station_search_api.application.usecase.searchlocation.result.SearchLocationResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchLocationsService implements SearchLocationsUseCase {
    private final SearchLocationPort searchLocationPort;

    @Override
    public List<SearchLocationResult> search(SearchLocationQuery query) {
        return searchLocationPort.search(query);
    }
}
