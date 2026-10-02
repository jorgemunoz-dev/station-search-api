# ADR-003: Canonical geographic areas

## Status

Accepted.

## Context

Postal search data, stations and calculated statistics previously stored their own
copies of locality and administrative-area names. Reads consequently joined on
normalized text and repeatedly executed `unaccent`, `lower` and regular-expression
normalization.

GeoNames postal rows are not geographic identities: one locality can have several
postal rows and every row owns a coordinate and accuracy. Conversely, country and
administrative areas do not naturally own a postal code.

## Decision

`geographic_area` is the canonical, multi-country hierarchy. Its internal
`BIGINT IDENTITY` key is independent from names and external sources. Areas have
one of `COUNTRY`, `ADMIN_AREA_1`, `ADMIN_AREA_2`, `ADMIN_AREA_3` or `LOCALITY`, a
parent, a normalized ingestion key, optional source code and normalized aliases.

`search_location` remains the replaceable postal/spatial projection and references
a canonical `LOCALITY`. `station` stores the four useful area IDs explicitly so
hot filters do not need recursive traversal. It retains its own ISO country code,
postal address and physical point, but no geographic names. Statistics identify
their scope with one `area_id`; price tables remain related only to stations.

Station ingestion first narrows candidates by country and postal code, then ranks
exact normalized locality matches, aliases and finally high-confidence trigram
matches, always requiring the supplied administrative hierarchy to match.

## Consequences

* Display names come from one catalogue.
* Station and statistics filters compare indexed IDs.
* GeoNames replacement never deletes or changes canonical IDs.
* A station can temporarily remain unresolved (nullable IDs) when the catalogue
  lacks a match; unresolved stations remain available to spatial searches but are
  excluded from area statistics until reconciled.
* Adding multiple identifiers per area may justify a separate identifier table in
  the future; it is not needed for the current single-source catalogue.
