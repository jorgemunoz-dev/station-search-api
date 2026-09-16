# ADR-001: Feature-first modular monolith

- Status: Accepted
- Date: 2026-09-16

## Context

The service currently follows a technically layered hexagonal structure: all use cases live below
`application/usecase`, all domain objects below `domain`, and all adapters below `infrastructure`.
This is simple initially, but every new capability adds files to the same package trees. It also
makes a feature change span distant folders and does not communicate ownership boundaries.

The service already contains three different business capabilities:

- maintaining and querying the station catalogue;
- searching geographical location suggestions;
- calculating and querying fuel-price statistics.

Station ingestion and station search operate on the same station catalogue and read model. They
are therefore two capabilities inside one **Station Catalogue** bounded context, rather than two
independent bounded contexts. Locations have their own language and persistence model. Statistics
has its own lifecycle and will eventually consume catalogue facts without owning stations.

## Decision

Use a **feature-first modular monolith**. Keep hexagonal layers inside each business module instead
of keeping one global folder per technical layer.

```text
com.petrolprice.station_search_api
├── station
│   ├── domain
│   │   ├── Station.java
│   │   ├── ProductPrice.java
│   │   └── ...
│   ├── ingestion
│   │   ├── application
│   │   │   ├── ProcessStationSnapshot.java
│   │   │   ├── CompleteStationPublishing.java
│   │   │   └── port
│   │   └── infrastructure
│   │       ├── rabbit
│   │       └── postgres
│   └── search
│       ├── application
│       │   ├── FindStations.java
│       │   └── port
│       └── infrastructure
│           ├── rest
│           └── postgres
├── location
│   ├── application
│   ├── domain
│   └── infrastructure
│       ├── rest
│       └── postgres
├── statistics
│   ├── application
│   ├── domain
│   └── infrastructure
│       ├── rest
│       └── postgres
└── platform
    ├── configuration
    └── observability
```

Package names describe business ownership first. `application`, `domain`, and `infrastructure`
remain useful, but only inside the module they protect.

### Dependency rules

1. A module's domain has no dependency on Spring, persistence entities, generated REST DTOs, or
   another module's infrastructure.
2. Infrastructure depends inward on its module's application ports and domain.
3. Modules do not import another module's infrastructure classes.
4. Cross-module collaboration occurs through an application API or a published domain event.
5. `platform` contains technical configuration only; it is not a dumping ground for shared domain
   concepts.
6. A concept is duplicated when it has different meanings in different contexts. For example,
   statistics may own `AnalyzedProduct` instead of importing a generated REST `ProductType`.

### Naming

Packages already supply context, so class names should express intent without redundant suffixes:

- input port: `FindStations`, implemented by `FindStationsService`;
- output port: `StationSearchRepository`, implemented by `PostgresStationSearchRepository`;
- command/query: `ProcessStationSnapshotCommand`, `FindStationsQuery`;
- adapters retain their technology in the name when it is useful.

`UseCase` and `Port` suffixes are allowed during migration, but new code should prefer the shorter
names above.

## Migration plan

The move should be incremental and behavior-preserving, not a single repository-wide rename:

1. **Statistics first.** It is mostly a skeleton today, so create new statistics code directly in
   `statistics` and prevent application code from depending on generated REST DTOs.
2. **Location search.** Move its application API, port, REST adapter, and PostgreSQL adapter as one
   vertical slice. Its limited dependencies make this a low-risk validation of the structure.
3. **Station catalogue.** Move the shared station domain, then ingestion and search application
   APIs, followed by their adapters. Keep ingestion and search as subpackages of `station`.
4. **Platform.** Move only truly cross-cutting Spring configuration and observability code.
5. Add automated package dependency tests after the packages exist; do not introduce rules that
   merely encode the old layout.

Each migration step must keep tests green and should not change API contracts, queue names, database
tables, or behavior in the same commit.

## Testing layout

Tests mirror their production module. Cross-module acceptance journeys remain under `integration`:

```text
src/test/java/.../
├── station/ingestion/...
├── station/search/...
├── location/...
├── statistics/...
└── integration/
    └── StationBatchSearchE2EIT.java
```

The batch-to-search test intentionally stays in `integration`: it verifies collaboration between
Rabbit ingestion and HTTP search and therefore does not belong exclusively to either adapter.

## Consequences

### Positive

- A feature can be found and changed in one package subtree.
- Package visibility can protect internal implementation details.
- Statistics and location can grow without enlarging a global `usecase` package.
- Boundaries are explicit without paying the operational cost of additional microservices.
- A module can be extracted later if its lifecycle genuinely diverges.

### Trade-offs

- Some technical concepts, such as PostgreSQL adapters, exist in multiple module trees.
- The station module remains intentionally larger because ingestion and search share one model.
- Moving existing code creates noisy imports, so migration must be separated from behavior changes.
- “Shared” abstractions require discipline; premature sharing would recreate the current coupling.

## Rejected alternatives

### Keep global technical layers

This preserves the current simplicity but continues to make business ownership less visible as the
number of use cases and adapters grows.

### One package or microservice per use case

Station ingestion and station search are not independent domains: both maintain or query the same
catalogue. Splitting them too early would duplicate models or introduce unnecessary contracts.

### Split statistics into a microservice now

There is no demonstrated independent scaling or deployment requirement yet. A module boundary gives
most of the design benefit and preserves the option to extract it later.
