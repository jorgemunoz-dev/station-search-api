package com.petrolprice.station_search_api.location.application;

import com.petrolprice.station_search_api.location.application.port.out.SearchLocationPort;
import com.petrolprice.station_search_api.location.application.query.SearchLocationQuery;
import com.petrolprice.station_search_api.location.application.result.SearchLocationResult;
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
