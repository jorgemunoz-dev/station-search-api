package com.petrolprice.station_search_api.application.usecase.findstations.query;

import lombok.Builder;

@Builder
public record FindStationsPageRequest(
    int page,
    int size
) {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 100;

    public FindStationsPageRequest {
        if (page < 0) {
            throw new IllegalArgumentException(
                "Page must be greater than or equal to 0"
            );
        }

        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException(
                "Size must be between 1 and " + MAX_SIZE
            );
        }
    }

    public long offset() {
        return (long) page * size;
    }

    public int fetchSize() {
        return size + 1;
    }
}
