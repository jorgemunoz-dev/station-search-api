# Integration testing approach

The integration suite is split into three layers so adding a case does not require copying JSON or SQL:

1. **Fixtures** create valid, unique data by default. A test overrides only values relevant to its scenario (`withBrand`, `withPrices`, etc.). Add a fluent method when a new input dimension is needed.
2. **Probes** expose business-oriented database observations and own SQL/table knowledge. Add a probe method instead of placing queries in a test.
3. **Scenario tests** invoke the nearest useful boundary:
   - `StationImportWorkflowIT` uses Spring services plus real PostgreSQL for fast ordering, idempotency and concurrency permutations.
   - `StationSnapshotRabbitIT` keeps only a small number of end-to-end messaging contract checks.
   - `StationBatchSearchE2EIT` is the acceptance test joining both sides of the application: a complete Rabbit batch is persisted, completed and then filtered through the HTTP API.
   - Adapter-specific tests should exercise PostgreSQL behavior directly when no service orchestration is involved.

All station fixtures generate snapshot, event and external IDs, brands, addresses and a valid location. Tests only fix values that are relevant to their assertion. Geospatial scenarios generate a random origin and describe the controlled relative offsets around it, so they remain deterministic without being tied to Madrid—or to any other real city. This prevents cases from depending on execution order and removes the shared station SQL dataset.

## Adding a workflow case

```java
StationSnapshotFixture snapshot = aStationSnapshot()
        .withBrand("CEPSA")
        .withPrices(price(DIESEL_A, "1.499"));

snapshotService.consume(snapshot.processCommand());
completionService.complete(snapshot.completionCommand(1));

assertThat(probe.importState(snapshot.snapshotId()).status())
        .isEqualTo("COMPLETED");
```

Use `runConcurrently` for race scenarios and Awaitility only across asynchronous boundaries such as RabbitMQ. Synchronous service tests should assert immediately, which makes failures faster and easier to diagnose.

## Current end-to-end coverage

| Application capability | Boundary covered | Test |
| --- | --- | --- |
| Process snapshot and complete import | Spring services -> PostgreSQL | `StationImportWorkflowIT` |
| Duplicate-event and finalization races | Concurrent service transactions -> PostgreSQL constraints/atomic updates | `StationImportWorkflowIT` |
| Snapshot and completion message contracts | RabbitMQ -> listeners -> workflow -> PostgreSQL | `StationSnapshotRabbitIT` |
| Complete batch and searchable result | RabbitMQ -> listeners -> PostgreSQL -> `GET /stations` | `StationBatchSearchE2EIT` |
| Location search | `GET /locations/search` -> PostgreSQL | `SearchLocationEndpointIT` |
| Station geospatial ranking details | Search adapter -> PostGIS | `PostgresStationSearchPersistenceAdapterIT` |

`GetFuelPriceSummaryService` and `FuelPriceStatisticsController` currently return `null`; they do not yet implement a behavior that an end-to-end test can assert. Their acceptance path should be added when statistics calculation and retrieval are implemented. Until then, the batch acceptance test verifies the available finalization behavior: the import reaches `COMPLETED` after all published stations are processed.
