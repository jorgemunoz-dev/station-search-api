package com.petrolprice.station_search_api.application.usecase.findstations.result;

import com.petrolprice.station_search_api.application.usecase.findstations.query.FindStationsPageRequest;

import java.util.List;

public record FindStationsResult (
    List<FindStationsItem> items,
    FindStationsPage page
) {
    public FindStationsResult {
        items = List.copyOf(items);
    }

    public static FindStationsResult empty(
        FindStationsPageRequest pageRequest
    ) {
        return new FindStationsResult(
            List.of(),
            new FindStationsPage(
                pageRequest.page(),
                pageRequest.size(),
                0,
                false,
                pageRequest.page() > 0
            )
        );
    }
}
