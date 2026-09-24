# ADR-002: Country and locality statistics

- Status: Implemented
- Date: 2026-09-24

## Context

The first statistics design introduced overlapping concepts such as geographic level, scope key,
area name, province, and municipality. Replacing those concepts with a separate administrative-area
catalogue would require another table and a new ingestion lifecycle even though `search_location`
already contains normalized locality names and optional `admin_area_1`, `admin_area_2`, and
`admin_area_3` metadata.

Clients need two common scopes: the whole country and a town or city selected through location
search. Administrative labels are useful for display and filtering, but they do not need to be a
second hierarchy owned by statistics.

## Decision

Use `(country_code, normalized_locality_name)` as the locality key shared by location search and
statistics:

- `countryCode` is always required;
- omitting `locality` selects country-wide statistics;
- supplying a locality selects its normalized name within that country;
- `GET /locations/search` returns `normalizedLocalityName` plus the three optional administrative
  area names and codes;
- statistics materialize one country row and one row per normalized locality and product;
- no `administrative_area` or station-to-area relation is created or maintained.

The statistics API accepts a human locality name and normalizes it with the same rules as location
search, so both `Ardales` and an already normalized value work. Clients should normally send the
`normalizedLocalityName` returned by location search.

`admin_area_1`, `admin_area_2`, and `admin_area_3` remain country-specific metadata. They are not
global enums and the service does not assign universal meanings such as state, province, or county.
The locality ranking endpoint can filter by those values without requiring them to identify a
locality.

## Snapshot calculation

Snapshot calculation maps each station to `search_location` by country and normalized postal code.
When a postal code contains multiple localities, the closest locality name is selected using the
existing trigram similarity index and source accuracy. Stations without a `search_location` match
remain part of country statistics but are not included in a locality aggregate.

This keeps statistics derived from data that the service already receives and avoids a catalogue
that must be populated independently.

## Consequences

### Positive

- Country and locality queries have one optional parameter instead of level-specific combinations.
- Location autocomplete provides the exact normalized value consumed by statistics.
- No new reference table, UUID discovery workflow, or manual administrative import is required.
- The three administrative fields retain country-specific context without constraining hierarchy.
- Country-wide, current, historical, ranking, summary, and saving use cases remain available.

### Trade-offs

- Locality names are assumed to be unique within a country, as required by the product decision.
- Stations that cannot be matched through country and postal code only contribute to country totals.
- Administrative-area aggregates beyond locality are filters/context, not independently materialized
  time series.
