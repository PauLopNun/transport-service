# Transport Service — Project Context

Supply Chain Workshop · GFT 2026 · MVP (without BREAKDOWN)

## Project links

| Resource | URL |
|---|---|
| Workshop repo (contracts + DDD source of truth) | https://github.com/PauLopNun/supply-chain-simulator-workshop |
| Trello board | https://trello.com/invite/b/69f3150f7f2afba50da43e35/ATTI8b3ee9a1af9383894332da7d3528aec1F238389E/supply-chain-workshop-gft-2026 |
| transport-service | https://github.com/PauLopNun/transport-service |
| simulation + map (Rubén) | https://github.com/fraarrmat11/MS-SIMULATION |
| factories / production (Idoia) | https://github.com/Fepe7/Factory-Workshop-Gft |
| warehouses (Pau) | https://github.com/Esmeralda6/Warehouse-Workshop |

---

## The supply chain system

Five microservices communicating via RabbitMQ:

| Service | Team | Role |
|---|---|---|
| **transport-service** | Iván, Pau López, Pau Greus | Manage trucks, assign shipments, move trucks on a grid |
| **simulation + map** | Rubén | Publishes `time.advanced.v1` on each tick · renders truck positions |
| **warehouses** | Pau | Core Domain · publishes `shipment.requested.v1` when stock is low |
| **production** | Idoia | Publishes `shipment.requested.v1` when factory needs materials |
| **reporting** | Pedro | Anticorruption layer · consumes everything · no writes |

Movement is Manhattan (X first, then Y). Speed = 1 step per simulation day.

---

## Transport service — tech stack

- Spring Boot 3.5.14 · Java 21 · Maven
- RabbitMQ (spring-boot-starter-amqp) — topic exchanges
- PostgreSQL + Liquibase
- Lombok · Springdoc OpenAPI
- Testcontainers (PostgreSQL + RabbitMQ) for integration tests
- Hexagonal architecture: domain / application / infrastructure

---

## Team and epic ownership

| Epic | Owner | GitHub | Layer |
|---|---|---|---|
| Trucks — Domain Model | **Iván** | inay | domain |
| Trucks — Use Cases | **Pau López** | pulz / paulopeznunez | application |
| Trucks — Messaging & Infrastructure | **Pau Greus** | pugz2 | infrastructure |

---

## What is implemented (updated 2026-05-05)

### Epic 1 — Domain Model (Iván) — COMPLETE
- `Truck` aggregate: TruckId, Location, TruckStatus, capacity, currentLoad, speed (default 1), deliveryIds
- `Delivery` aggregate: DeliveryId, shipmentId (UUID), truckId, origin, destination, items, assignedAt, completedAt — with `isArrived(Location)` and `complete(int)` domain methods
- Domain events: TruckRegisteredEvent, TruckStatusChangedEvent, TruckPositionUpdatedEvent, DeliveryCompletedEvent — all timestamps are `int` (simulation day)
- Repository ports: TruckRepository, DeliveryRepository (interfaces)
- Domain services: DistanceCalculator (Manhattan distance + `isOnRoute`), OptimalTruckSelector

### Epic 2 — Use Cases (Pau López) — COMPLETE
- RegisterTruck use case + POST /trucks + GET /trucks
- AssignTruck use case (DISPATCHED + LOAD_UPDATED for IN_TRANSIT fallback)
- AdvanceTrucks use case (moves trucks, completes deliveries, RETURNED_TO_BASE)
- DeliveryRepositoryAdapter + DeliveryEntity + Liquibase migrations
- Output ports: TruckEventPublisher, DeliveryEventPublisher (interfaces only — implementations pending Epic 3)
- ~60 unit tests + 5 IT tests (DeliveryRepositoryAdapterIT)

### Epic 3 — Messaging & Infrastructure (Pau Greus) — PENDING
- TruckEventPublisher implementation (RabbitMQ)
- DeliveryEventPublisher implementation (RabbitMQ)
- TruckEntity + TruckJpaRepository + TruckRepositoryAdapter + Liquibase trucks table
- DispatchRequestedListener (consumes `shipment.requested.v1` → calls AssignTruck)
- TimeAdvancedListener (consumes `time.advanced.v1` → calls AdvanceTrucks)
- RabbitMQConfig (exchanges, queues, bindings)
- application.yml (DB + RabbitMQ config)
- DispatchRequestedListenerIT + TimeAdvancedListenerIT (Testcontainers)

---

## Messaging contracts (MVP — confirmed against workshop repo)

### PUBLISHED by transport-service

**truck.registered.v1** → Reporting, Map UI
```json
{ "truckId": "uuid", "name": "Truck 01", "location": {"x":0,"y":0}, "capacity": 10, "timestamp": 0 }
```
Note: trucks register before simulation starts → timestamp = 0

**truck.status.changed.v1** → Reporting
```json
{ "truckId": "uuid", "oldStatus": "AVAILABLE", "newStatus": "IN_TRANSIT", "position": {"x":0,"y":0}, "currentLoad": 6, "capacity": 10, "timestamp": 3, "reason": "DISPATCHED" }
```
Reasons: `TRUCK_REGISTERED` (oldStatus null) · `DISPATCHED` · `LOAD_UPDATED` · `RETURNED_TO_BASE`
Status cycle: AVAILABLE → IN_TRANSIT → AVAILABLE (DELIVERED exists in enum but is not used in current flow)

**truck.position.updated.v1** → Map UI, Reporting
```json
{ "truckId": "uuid", "location": {"x":5,"y":3} }
```
High frequency — no business data, no timestamp.

**delivery.completed.v1** → Warehouses, Reporting, Map UI
```json
{ "shipmentId": "uuid", "truckId": "uuid", "items": [{"materialType":"wood","quantity":6}], "location": {"x":8,"y":2}, "completedAt": 5 }
```

### CONSUMED by transport-service

**time.advanced.v1** ← Rubén (Simulation)
```json
{ "currentDay": 3 }
```
Note: Rubén's service actually publishes `{ previousDay, currentDay, daysAdvanced, eventId, occurredAt }`.
`daysAdvanced` is pre-calculated — TimeAdvancedListener can use it directly (no need to track lastDay).
Confirm with Rubén which fields are safe to consume.

**shipment.requested.v1** ← Warehouses / Factories
```json
{ "shipmentId": "uuid", "originId": "warehouse-north-01", "destinationId": "warehouse-south-03", "items": [{"materialType":"wood","quantity":6}], "requestedAt": 3 }
```
`originId` and `destinationId` are string IDs. **DispatchRequestedListener is responsible for resolving them to Location coordinates** before calling AssignTruck.

---

## REST endpoints

| Method | Path | Description |
|---|---|---|
| POST | /trucks | Register new truck. Body: CreateTruckRequest |
| GET | /trucks | Get all trucks with current location and status |

Full spec: `src/main/resources/openapi.yaml` · Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Key architectural decisions

- Truck and Delivery aggregates are **immutable** (Lombok @Builder). Updating state = rebuild with builder — always preserve all fields including `speed`.
- DeliveryItem and Location are **records** (Java 21 value objects, no ID, equality by value).
- `AdvanceTrucks.execute(int daysAdvanced, int currentDay)` — lastDay tracking lives in TimeAdvancedListener, not in the use case.
- Arrival detection: `delivery.isArrived(truckLocation)` — domain logic belongs on the aggregate.
- Delivery completion: `delivery.complete(currentDay)` — returns new immutable Delivery with completedAt set.
- `isOnRoute(point, from, to)` on DistanceCalculator checks if a point is on the L-shaped Manhattan path (X-leg first, then Y-leg). Used by DispatchRequestedListener for the IN_TRANSIT truck fallback.

---

## Open items (to coordinate before Epic 3 starts)

- [ ] `materialType` naming convention — must align with team-warehouses and team-factories
- [ ] Reporting (Pedro) expects `truck.assigned.v1` and `delivery.created.v1` — resolved: `truck.assigned.v1` maps to `truck.status.changed.v1 reason=DISPATCHED` (same event, different name in his doc). `delivery.created.v1` is a warehouse responsibility (`shipment.requested.v1`), not transport's. Pedro needs to update his listeners accordingly.
- [ ] `DELIVERED` TruckStatus value exists in enum but is not used in any flow. Remove or keep?
- [ ] `reasonCode` (typed enum for BREAKDOWN phase) — deferred to post-MVP

---

## Working conventions

- TDD: test committed before implementation, separate commits (`test: ...` then `feat: ...`)
- One logical unit per commit — small, focused
- 100% coverage enforced by JaCoCo on ALL production classes (config, entities, and main application excluded — these are framework boilerplate with no logic). `lombok.config` has `addLombokGeneratedAnnotation=true` so JaCoCo skips Lombok-generated bytecode.
- No future-dated commits — spread across working hours 08:00-17:30
- Branch naming: `type/short-description` — e.g. `feature/truck-assignment`, `fix/stock-checker-null-pointer`
- Commit format: `type: short description` — e.g. `feat: add optimal truck selector`, `fix: handle null stock`
- Valid types: `feat` `fix` `chore` `docs` `test` `refactor`

---

## Clean Code & SOLID — non-negotiable rules

This project follows Uncle Bob's *Clean Code* strictly. Every PR must comply.

### Naming
- Names must reveal intent — a reader should understand the purpose without reading the implementation.
- Variables: noun phrases (`pendingDeliveries`, `totalItemCount`). Booleans: predicate form (`isAssigningToInTransitTruck`).
- Methods: verb phrases (`calculateNextStep`, `completeDelivery`, `returnTruckToBase`).
- No abbreviations, no single-letter variables outside loop indices.
- No misleading names — a `List` named `data` or a boolean named `flag` is a bug in readability.

### Comments
- Do NOT write comments that describe WHAT the code does — well-named identifiers already do that.
- Comments are reserved for the non-obvious WHY: a hidden constraint, a domain invariant, a known workaround.
- Trello card references, task IDs, and implementation notes belong in commit messages and PRs — never in source code.
- If a comment feels necessary, first try renaming the code to make the comment unnecessary.

### Functions / Methods
- One responsibility per method. If you can describe a method with "and", split it.
- Methods should be small — aim for under 15 lines. If longer, extract private helpers.
- Arguments: prefer 0–2. More than 3 is a code smell — consider a command/parameter object.
- No side effects hidden inside a method that does something else.

### SOLID
- **S** — Single Responsibility: each class/method has one reason to change.
- **O** — Open/Closed: domain and use-case logic is extensible without modifying existing classes.
- **L** — Liskov Substitution: all port implementations (repository adapters, publishers) are interchangeable with their interfaces.
- **I** — Interface Segregation: ports are narrow — `TruckEventPublisher` publishes truck events only, not delivery events.
- **D** — Dependency Inversion: use cases depend on interfaces (ports), never on infrastructure directly.

### DDD alignment
- Domain layer has zero infrastructure imports (`javax.persistence`, `org.springframework`, etc.).
- Domain events carry only domain types — no JSON, no wire types.
- Aggregates (`Truck`, `Delivery`) are immutable (`@Builder`). Mutation = rebuild via builder.
- Value objects (`Location`, `DeliveryItem`) are Java records — equality by value, no identity.
- Use case classes (`RegisterTruck`, `AssignTruck`, `AdvanceTrucks`) are thin orchestrators — they delegate logic to the domain.
