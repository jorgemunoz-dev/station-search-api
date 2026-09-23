# ADR-002: Country-neutral administrative areas

- Status: Implemented
- Date: 2026-09-23

## Context

The statistics contract currently describes a geographical selection with three values:
`level`, `area`, and `province`. Internally, the same selection is represented by
`GeographicScope(level, name, province)` and persisted as four partly overlapping columns:
`geographic_level`, `scope_key`, `area_name`, and `province`.

This creates several problems:

- `area` changes meaning with `level`, while `province` is both an area name and the parent of a
  municipality;
- `scope_key` encodes hierarchy and identity in an undocumented, case-normalized string, even though
  the other columns repeat the same information;
- `GeographicLevel` and the `/provinces` endpoint expose Spain's administrative vocabulary as part
  of the domain and public API;
- names are used as identifiers, so spelling, language, accents, renames, and duplicate place names
  can split or merge aggregates incorrectly;
- adding a country with states, regions, departments, counties, districts, or a different number of
  hierarchy levels requires changing the enum, API, SQL, and schema together.

The API must keep its current behaviour while allowing administrative structures that are not
province/municipality based.

## Decision

Introduce **administrative area** as the single country-neutral concept. An area has stable identity,
display metadata, and an optional parent:

```text
administrative_area
├── id                 UUID (internal stable identity)
├── country            ISO 3166-1 alpha-2
├── source             provider/registry namespace
├── external_code      stable code within source and country
├── type               country-owned value such as province, state, county, municipality
├── name               preferred display name
├── normalized_name    search aid, never identity
└── parent_id           nullable FK to administrative_area.id
```

`type` is descriptive metadata, not a closed global hierarchy enum. Parent relationships define
the hierarchy. A country may therefore have any number of levels and use its own terminology without
a release of this service. The unique external identity is `(country, source, external_code)`.

Stations are associated with all known containing areas through a `station_administrative_area`
relation. Keeping the complete ancestry makes grouping inexpensive and avoids assuming that a
station always has exactly one parent chain. Ingestion is responsible for supplying stable area
codes and relationships; free-text address fields remain presentation data during migration.

Statistics refer to a nullable `administrative_area_id`:

- `NULL` means the whole country;
- a UUID means that exact area, independently of its name or type.

The uniqueness and lookup dimensions become
`(snapshot_id, country, product_type, administrative_area_id)`. `scope_key`, `geographic_level`,
`area_name`, and `province` are then projections or migration data, not independent sources of
truth. Responses obtain the current name, type, and ancestors from the area catalogue.

### Application model

Replace the level-dependent `GeographicScope` shape with an identity-based scope:

```java
sealed interface GeographicScope {
    record Country(String countryCode) implements GeographicScope {}
    record AdministrativeArea(String countryCode, UUID areaId) implements GeographicScope {}
}
```

Country remains explicit so an area from one country cannot accidentally be queried under another.
Use cases that rank subdivisions accept an optional `parentAreaId` and optional `type`, rather than
having a province-specific method.

### HTTP contract

Add generic resources without immediately removing existing clients:

- `GET /administrative-areas?countryCode=ES&parentId=...&type=...` discovers areas;
- `GET /statistics/fuel-prices/current?countryCode=ES&areaId=...` selects one area;
- `GET /statistics/fuel-prices/history?countryCode=ES&areaId=...` uses the same selection;
- `GET /statistics/fuel-prices/administrative-areas?countryCode=ES&parentAreaId=...&type=...`
  ranks comparable areas.

Omitting `areaId` retains national scope. The API returns an area reference containing `id`,
`countryCode`, `type`, `name`, and, where useful, its ancestor path. Clients display labels but send
IDs back; they do not construct scope keys or infer hierarchy.

The existing `level`, `area`, and `province` parameters and `/provinces` endpoint remain temporarily
available as deprecated compatibility adapters. They resolve legacy names to an area ID and invoke
the same application use case. Ambiguous legacy names return a validation error instead of choosing
silently. Existing response fields may remain during this window, but new clients use the area
reference.

## Implementation record

The first-version schema was changed in place because it has not been released. Statistics now store
only `administrative_area_id`; there is no dual-write of `scope_key`, `geographic_level`, `area_name`,
or `province`. Snapshot calculation materializes a legacy address catalogue from the existing station
fields, relates stations to every known containing area, and aggregates by stable area ID.

Generic discovery, current, historical, and ranking reads use area IDs. The existing name parameters
and `/provinces` route are marked deprecated but resolve to the same IDs and use the same query path.
This preserves current clients while allowing them to migrate incrementally.

The current Rabbit contract does not yet carry provider-owned administrative codes. Until it does,
the compatibility importer creates deterministic `legacy-address` external codes from normalized
province and municipality paths. A future ingestion change can add authoritative areas without
changing the statistics schema or generic HTTP API.

## Invariants

- Names and normalized names are never identifiers.
- The service does not define a universal order for administrative types.
- An area's parent, when present, belongs to the same country.
- Area identity is stable across translations and display-name changes.
- National scope is represented once by the absence of an area, not by a sentinel string.
- Public clients never need to know or construct a persistence key.

## Consequences

### Positive

- One concept replaces the overlapping level, key, area-name, and province concepts.
- New countries and administrative levels become data changes rather than Java enum and API changes.
- Stable codes prevent case, spelling, translation, and same-name collisions.
- Current, historical, and ranking APIs use the same selection mechanism.
- Compatibility is preserved while consumers migrate.

### Trade-offs

- Ingestion must source or map reliable administrative codes and hierarchy data.
- Resolving and joining area IDs adds schema and operational complexity during migration.
- A many-to-many station relation uses more rows than fixed province/municipality columns.
- Legacy name resolution cannot always be lossless; ambiguous data needs explicit remediation.

## Rejected alternatives

### Rename province to region and keep the enum

This improves vocabulary but retains a fixed number of levels and still makes names act as identity.

### Generalize `scope_key`

A key such as `type:parent:name` remains a second, encoded hierarchy model. Escaping, renames,
collation, and aliases make it fragile, and exposing it would couple clients to storage decisions.

### Store an arbitrary hierarchy only as JSON

JSON is useful as source metadata, but making it the sole model weakens referential integrity and
makes aggregate joins and hierarchy queries harder. Stable relational identity plus optional source
metadata provides a clearer boundary.
