# Snapshot-based fuel-price statistics

## Calculation lifecycle

Statistics are not calculated from `station_current_product_price` when an HTTP request arrives.
Every historical price row records the `snapshot_id` that produced it. Once every event from a
snapshot has been processed, `StationImportFinalizer` atomically claims the import, invokes
`CalculateFuelPriceStatisticsUseCase`, and marks the import `COMPLETED` only after calculation
succeeds.

The calculator scans the historical prices belonging to that snapshot and materializes national and
administrative-area aggregates in `fuel_price_statistics`. Existing station address values bootstrap
the administrative-area catalogue. A failure rolls back both the
statistics and the import state, allowing Rabbit retry to run the whole operation again. Replacing
rows by snapshot makes calculation idempotent.

HTTP queries read the latest materialized snapshot instead of repeatedly aggregating the operational
current-price table. Historical endpoints read the materialized snapshots over time. Radius queries
cannot be precomputed for arbitrary coordinates, so they use the price rows from the latest
calculated snapshot and PostGIS; they never use `station_current_product_price`.

## Available statistics

For each product, current snapshot aggregates provide average, minimum, maximum, station count,
cheapest/most-expensive station, national and parent-area differences where applicable, and generic
administrative-area rankings. Address-derived child areas use `municipality` and fall back to
`locality` when municipality is null.

Historical results expose the calculated aggregates for each completed snapshot/day, period
average/minimum/maximum, and exact 1/7/30-day absolute and percentage variations. Missing reference
dates return `null`; prices are not interpolated.

Radius statistics support 5/10/20/50 km and return station count, average, minimum, maximum, price
spread, cheapest station, and nearest station from the latest calculated snapshot.

Theoretical savings use the station price from the latest calculated snapshot and a supplied
reference price. They return per-litre and per-tank savings and default to 55 litres.

## Not derivable reliably

- **Authoritative administrative identity:** existing events only provide province and municipality
  names, so imported `station-address` codes cannot eliminate upstream aliases. Provider or registry
  codes are needed for canonical comparisons and additional hierarchy levels.
- **Continuous daily history:** a missing snapshot date cannot be reconstructed.
- **Sales-weighted prices:** there is no sales volume, so averages are station-weighted.
- **Realized consumer savings:** there is no purchase, route, vehicle-consumption, or fill data.

## API

- `GET /statistics/fuel-prices/current`
- `GET /statistics/fuel-prices/history`
- `GET /statistics/fuel-prices/around`
- `GET /statistics/fuel-prices/administrative-areas`
- `GET /administrative-areas`
- `GET /statistics/fuel-prices/savings`
- `GET /statistics/fuel-prices/summary`

Current, historical, calculation, and geospatial access use statistics-owned ports. No statistics
query depends on `StationSearchRepositoryPort`, and all aggregation is set-based to avoid N+1 access.

## Indexes

The changelogs add a snapshot/product/station index for imported historical prices, a lookup index on
materialized statistics, area indexes, and product-first covering indexes. Radius queries reuse the
GiST index on `station.location`.
