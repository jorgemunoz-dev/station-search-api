# ADR-001: Feature-first modular monolith

- Status: Implemented
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
│   │       └── rabbit
│   ├── search
│       ├── application
│       │   ├── FindStations.java
│       │   └── port
│       └── infrastructure
│           └── rest
│   └── infrastructure
│       └── postgres
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
├── contract
│   └── rest (OpenAPI-generated API and models)
└── platform
    ├── configuration
    ├── observability
    └── rest (cross-cutting error handling)
```

Package names describe business ownership first. `application`, `domain`, and `infrastructure`
remain useful, but only inside the module they protect.

Station PostgreSQL persistence sits at `station.infrastructure.postgres` because ingestion writes
the same catalogue model that search reads. Rabbit and REST adapters remain inside their respective
capabilities. This makes the shared read model explicit without allowing one capability to depend on
the other's adapter.

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

## Migration record

The package migration was completed as behavior-preserving vertical slices:

1. **Statistics** now owns its application API, REST adapter, and product dimension; application code
   no longer depends on generated REST DTOs.
2. **Location search** owns its application API, output port, REST adapter, and PostgreSQL adapter.
3. **Station catalogue** owns the shared station domain, ingestion and search application APIs,
   Rabbit adapter, REST adapter, and shared PostgreSQL read/write model.
4. **Platform** owns cross-cutting Spring configuration, REST error handling, and observability.
5. Unit tests mirror their production modules; only cross-module journeys remain in `integration`.

The migration does not change API contracts, queue names, database tables, or application behavior.

## Station ingestion to statistics collaboration

`StationImportFinalizer` uses a **synchronous application API** after the atomic readiness claim.
It does not depend on statistics infrastructure and does not publish an in-memory Spring event:

```text
station.ingestion.application.StationImportFinalizer
    -> statistics.application.CalculateFuelPriceStatistics
    -> statistics infrastructure through statistics-owned output ports
```

The statistics module owns the input port and command:

```java
public interface CalculateFuelPriceStatistics {
    void calculate(CalculateFuelPriceStatisticsCommand command);
}
```

The finalization transaction follows this order:

1. Atomically change `PROCESSING` to `CALCULATING_STATISTICS` only when publishing has completed and
   `processedStations == publishedStations`.
2. If the claim returns `false`, finish without doing any work.
3. Call the statistics application API with the snapshot identifier.
4. Mark the import `COMPLETED` only after calculation succeeds.

Because both modules run in the same process and use the same database, the synchronous call joins
the existing transaction. If calculation fails, the claim and completion transition roll back and
the Rabbit delivery can be retried. The atomic status transition ensures that concurrent finalizers
cannot calculate the same snapshot twice.

An asynchronous event is not justified yet. Publishing an ordinary Spring event after the claim
would risk losing work if the process stops, while publishing RabbitMQ directly inside the database
transaction would create a dual-write problem. If statistics later needs independent workers or a
separate deployment, replace the direct application call with a **transactional outbox**: persist a
`StationImportReadyForStatistics` event in the same transaction as the claim, relay it to RabbitMQ,
make the statistics consumer idempotent by `snapshotId`, and mark the import completed only after a
successful calculation.

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
