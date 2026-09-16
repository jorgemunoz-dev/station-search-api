# Integration testing approach

The integration suite is split into three layers so adding a case does not require copying JSON or SQL:

1. **Fixtures** create valid, unique data by default. A test overrides only values relevant to its scenario (`withBrand`, `withPrices`, etc.). Add a fluent method when a new input dimension is needed.
2. **Probes** expose business-oriented database observations and own SQL/table knowledge. Add a probe method instead of placing queries in a test.
3. **Scenario tests** invoke the nearest useful boundary:
   - `StationImportWorkflowIT` uses Spring services plus real PostgreSQL for fast ordering, idempotency and concurrency permutations.
   - `StationSnapshotRabbitIT` keeps only a small number of end-to-end messaging contract checks.
   - Adapter-specific tests should exercise PostgreSQL behavior directly when no service orchestration is involved.

All fixtures use generated snapshot, event and external IDs. This prevents cases from depending on execution order and avoids maintaining a large shared dataset. Fixed SQL datasets remain useful for search/ranking tests, where a carefully controlled spatial distribution is the behavior under test.

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
