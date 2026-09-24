# Snapshot-based fuel-price statistics

## Calculation lifecycle

Statistics are calculated from historical prices after a snapshot finishes. Each snapshot produces
one aggregate per country/product and one aggregate per matched normalized locality/product. A
failure rolls back statistics and import completion, so Rabbit retry can safely repeat the operation.

Locality assignment reuses `search_location`: station country and normalized postal code identify
candidates, while locality similarity and source accuracy resolve postal codes shared by multiple
places. A station with no match still contributes to its country aggregate.

## Available statistics

- Current country or locality average, minimum, maximum, station count, and cheapest/most-expensive
  station.
- Difference between a locality average and its country average.
- Difference between a locality and its `admin_area_2` weighted average where metadata exists.
- Historical daily aggregates and exact 1/7/30-day absolute and percentage variations.
- Locality rankings, optionally filtered by `adminArea1`, `adminArea2`, or `adminArea3`.
- Radius statistics and theoretical station savings.

## API

- `GET /locations/search` returns `normalizedLocalityName` and administrative context.
- `GET /statistics/fuel-prices/current`
- `GET /statistics/fuel-prices/history`
- `GET /statistics/fuel-prices/localities`
- `GET /statistics/fuel-prices/around`
- `GET /statistics/fuel-prices/savings`
- `GET /statistics/fuel-prices/summary`

`countryCode` is required. The current and historical endpoints use country scope when `locality` is
omitted and locality scope when it is supplied. No separate administrative catalogue is required.
