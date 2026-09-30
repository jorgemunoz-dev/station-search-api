# Snapshot-based fuel-price statistics

## Calculation lifecycle

Statistics are calculated from historical prices after a snapshot finishes. Each snapshot produces
one aggregate per country/product, normalized locality/product, and available positional
administrative area/product. A
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
- Current and historical queries for `adminArea1`, `adminArea2`, or `adminArea3`.
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

`countryCode` is required. Current and historical queries select exactly one optional scope:
`locality`, `adminArea1`, `adminArea2`, or `adminArea3`; omitting all four selects the country. No
separate administrative catalogue is required.

The locality-ranking endpoint always keeps locality granularity: each response element aggregates
all matched stations for one `normalizedLocalityName`. Administrative parameters are cumulative
filters over `search_location` metadata, not locality selectors. Consequently, several localities
may be returned for one `adminArea3` value. To retrieve exactly one town or city, use `locality` on
the current or historical endpoint; `stationCount` then reports how many stations contributed to
that aggregate.
