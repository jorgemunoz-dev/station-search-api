package com.petrolprice.station_search_api.statistics.application.port.out;

import com.petrolprice.station_search_api.statistics.application.query.CurrentStatisticsQuery;
import com.petrolprice.station_search_api.statistics.application.result.AdministrativeArea;
import com.petrolprice.station_search_api.statistics.application.result.CurrentPriceStatistics;
import com.petrolprice.station_search_api.statistics.application.result.RankedAreaStatistics;
import com.petrolprice.station_search_api.statistics.domain.ProductType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurrentPriceStatisticsRepository {
    Optional<CurrentPriceStatistics> current(CurrentStatisticsQuery query);

    List<RankedAreaStatistics> areas(
            String countryCode, ProductType productType, UUID parentAreaId, String areaType);

    List<AdministrativeArea> findAreas(String countryCode, UUID parentAreaId, String areaType);

    Optional<BigDecimal> stationPrice(UUID stationId, ProductType productType);
}
