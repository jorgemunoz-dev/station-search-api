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

## Package Structure Proposed

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