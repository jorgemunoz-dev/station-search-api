# Statistics supported by the current data model

## Available now

The current-price table contains one row per station and product, so it can provide unbiased current
aggregates for country, province, municipality/locality and geographic radius. The statistics module
implements average, minimum, maximum, station count, cheapest/most expensive station, national and
provincial differences where applicable, province rankings, and nearest/cheapest stations inside
5/10/20/50 km radii.

Theoretical savings are derivable from a station price and a supplied reference price. The
`FuelSavingCalculator` returns both per-litre and per-tank savings and uses 55 litres when tank size
is omitted. A negative result is deliberately retained: it means the selected station is more
expensive than the reference.

Historical calculations use `historical_product_price`. To prevent stations updated many times in one
day from receiving extra weight, queries select the last observation per station/product/day before
calculating the daily aggregate. Variations compare with an exact observation date 1, 7, or 30 days
before; the value is `null` when that date has no data rather than inventing or interpolating a price.

Municipality statistics require their province, use `municipality`, and fall back to `locality` only
when municipality is null. Names are compared case-insensitively. This works with current data but canonical administrative IDs
would be safer than names.

## Not derivable reliably

- **Autonomous communities:** `station` has province but no autonomous-community field or stable
  province code. A hardcoded province mapping would be reference data not present in the current
  model. Add `autonomous_community_code` and `autonomous_community_name` (ideally populated by the
  ingestion service) before exposing these statistics.
- **Canonical municipality comparisons:** free-text municipality/locality values can contain aliases,
  spelling or casing differences. Add INE municipality/province codes to make grouping exact.
- **Continuous daily history:** missing dates cannot be reconstructed from observations. Exact
  1/7/30-day variations remain null when there was no snapshot that day.
- **Sales-weighted prices:** no litres sold or transaction volume exists, so all averages are
  station-weighted.
- **Realized consumer savings:** only theoretical savings can be calculated because there is no
  purchase, route, vehicle consumption or tank-fill data.

## API/query model

- `GET /statistics/fuel-prices/current`: `NATIONAL`, `PROVINCE`, or `MUNICIPALITY` current aggregate.
- `GET /statistics/fuel-prices/history`: daily time series for the same levels and a date range.
- `GET /statistics/fuel-prices/around`: aggregate for an allowed radius and coordinates.
- `GET /statistics/fuel-prices/provinces`: all province aggregates with cheapest and most-expensive
  ranks, suitable for province comparisons.
- `GET /statistics/fuel-prices/savings`: theoretical saving for a station/product against a supplied
  reference price, with an optional tank size.
- The existing `/statistics/fuel-prices/summary` endpoint now uses the national aggregate and history.

Database access is split into `CurrentPriceStatisticsRepository`, `HistoricalPriceStatisticsRepository`, and `GeospatialPriceStatisticsRepository`; station search persistence remains
separate. Queries aggregate in PostgreSQL/PostGIS and fetch extrema in the same statement, avoiding
N+1 access.

## Indexes

Changelog `011-create-statistics-indexes.yaml` adds product-first covering indexes for current and
historical prices and functional country/area indexes for case-insensitive province and municipality
filters. Radius queries reuse the existing GiST index on `station.location`.
