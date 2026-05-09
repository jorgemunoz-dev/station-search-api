# station-search-api Architecture

## Overview

`station-search-api` is the microservice responsible for storing and exposing data related to fuel stations and EV charging stations.

Its purpose is to provide an REST-API only accessible by the frontend. This API  allow filtering stations by:
- Current prices
- Historical prices
- Compare two stations
- Find the cheapest station in a geospatial range

---

## Context

This service is part of a  system composed of:
- `station-data-ingestion`: collects and normalizes  data from external providers
- `station-search-api`: expose a query API optimized for frontend consumption

The ingestion service publishes events that are consumed by this service to build a query-optimized  read model.

---

## Data flow

1. `station-data-ingestion` retrieve raw data from external providers
2. The ingestion service normalizes and publishes events
3. `station-search-api` consumes these events
4. The service updates:
    - current prices (fast reads)
    - historical prices (analytics)
5. The fronted queruies this service via REST API

---


## Responsabilities

The main responsabilities of this service are:
* consume events from station-data-ingestion microservice
* store snapshot info on database
* expose REST API to query this data from the frontend

----

## Architecture Style

This service follows the **Hexagonal Architecture**

The main goal is to keep the domain isolated from technical concerns suhc as:
* HTTP controllers
* persistence frameworks
* messaging brokers

---

## Package Structure

```text 
com.petrolprice.station_search_api
├── StationSearchApiApplication.java
├── domain
│   ├── model
│   ├── service
│   └── port
│       ├── in
│       └── out
├── application
│   ├── usecase
│   ├── command
│   └── mapper
└── infrastructure
    ├── in
    │   ├── rest
    │   └── messaging
    ├── out
    │   └── persistence
    └── config
```

---

## Naming convention

To keep consistency across the code, the following naming conventions are used:

### Domain
- Entities and value objects
  - `Station`, `FuelPrice`, `Money`
- Aggregation / input models:
  - `StationUpdate`
- Domain services:
  - `BestOptionSelector`, `SavingsCalculator`, `StationScoringPolicy`

### Application
- Use cases:
  - Suffix: `UseCase`
  - Examples:
    - `FindStationUseCase`
    - `FindBestOptionUseCase`
    - `ConsumeStationSnapshotUseCase`
- Commands / Queries:
  - Suffinx: `Command`, `Query`
  - Example: 
    - `FindStationQery`
    - `CompareStationsQuery`
- Mappers:
  - Suffix: `Mapper`
  - Example:
    - `StationQueryMapper`
- Ports:
  - Output ports:
    - Suffix: `{technology}Port`
    - Example:
      - `StationRepositoryPort`
      - `HistoricalFuelPriceRepositoryPort`

### Infrastructure
#### REST

- Controllers:
   - Suffix: `Controller`
   - Example:
      - `StationController`

- Request / Response DTOs:
   - Suffix: `Request`, `Response`
   - Examples:
      - `FindStationsRequest`
      - `StationResponse`

---

#### Messaging (RabbitMQ)

- Message DTOs:
   - Suffix: `Message`
   - Example:
      - `StationSnapshotMessage`

- Listeners:
   - Suffix: `Listener`
   - Example:
      - `StationSnapshotRabbitListener`

- Message mappers:
   - Suffix: `MessageMapper`
   - Example:
      - `StationSnapshotMessageMapper`

---

#### Persistence

- JPA entities:
   - Suffix: `Entity`
   - Example:
      - `StationEntity`
      - `CurrentFuelPriceEntity`

- Spring Data repositories:
   - Suffix: `JpaRepository`
   - Example:
      - `StationJpaRepository`

- Persistence adapters:
   - Suffix: `PersistenceAdapter`
   - Example:
      - `PostgresStationPersistenceAdapter`

- Entity mappers:
   - Suffix: `EntityMapper`
   - Example:
      - `StationEntityMapper`
---

## Layer Responsibilities

### Domain

The domain layer contains the core business concepts of  the search api service.

Typical contents:

* models such as `Station`, `Money`, `Distance`, `SearchCriteria`
* value objects such as `Coordinates`, `FuelType`, `Price`
* domain events for business  rules such as  best option selection or savings calculation 
* ports that represent what the domain/application needs from the outside world

### Application

The application layer coordinates use cases.

* use cases such as `FindStationsUseCase`, `FindBestOptionUseCase`, `CompareStationsUseCase`
* commands and queries used as input  models
* application mappers when needed

This layer should orchestrate behavior, not contain low-level parsing details or frameworks specification code.

### Infrastructure

The infrastructure layer contains technical adapters.

* REST controllers
* RabbitMQ listeners
* persistence adapters
* database entities
* DTOs related to external services
* Spring configurations

This is the only place where providers and frameworks code should live.

## Roadmap

See [ROADMAP.md](./ROADMAP.md)