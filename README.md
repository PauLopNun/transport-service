# Transport Service

**Supply Chain Workshop | GFT | May 2026**
MVP Version — without BREAKDOWN

---

## Team

| Epic | Owner | GitHub |
|---|---|---|
| Domain Model (Truck & Delivery aggregates) | **Iván** | inay |
| Use Cases (RegisterTruck, AssignTruck, AdvanceTrucks) | **Pau López** | pulz / paulopeznunez |
| Messaging & Infrastructure (RabbitMQ, persistence) | **Pau Greus** | pugz2 |

---

## System Overview

Five microservices communicate via RabbitMQ on a shared CloudAMQP broker:

| Service | Team | Role |
|---|---|---|
| **transport-service** | Iván, Pau López, Pau Greus | Manage trucks, assign shipments, move trucks on a grid |
| **simulation + map** (ms-time / ms-map) | Rubén | Publishes `time.advanced.v1` · renders truck positions |
| **warehouses** | Pau | Core Domain · publishes `shipment.requested.v1` when stock is low |
| **production** | Idoia | Publishes `shipment.requested.v1` when factory needs materials |
| **reporting** | Pedro | Anticorruption layer · consumes everything · no writes |

Movement is Manhattan (X first, then Y). Speed = 1 step per simulation day.

---

## Tech Stack

- Spring Boot 3.5.14 · Java 21 · Maven
- RabbitMQ (`spring-boot-starter-amqp`) — topic exchanges
- PostgreSQL + Liquibase
- Lombok · Springdoc OpenAPI (`http://localhost:8080/swagger-ui.html`)
- Testcontainers (PostgreSQL + RabbitMQ) for integration tests
- Hexagonal architecture: domain / application / infrastructure

---

## Running the Service

### With CloudAMQP (shared broker)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=cloudamqp
```

Set the RabbitMQ password via environment variable or `.env` file:

```
RABBITMQ_PASSWORD=your_password
```

### Local (Docker)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

---

## End-to-End Verification Script

`verify-e2e.ps1` tests the full service behaviour end-to-end against the shared CloudAMQP broker. It registers a truck, publishes the required upstream events, and verifies every published event and state change.

```powershell
.\verify-e2e.ps1 -Password "your_cloudamqp_password"

# Against the AWS deployment
.\verify-e2e.ps1 -Password "your_cloudamqp_password" -ServiceUrl "http://taller-deploy-aws-2026-nlb-pulz-adee7212e878911f.elb.eu-west-1.amazonaws.com:8080"
```

By default the script purges stale messages from Transport input queues in CloudAMQP before publishing the E2E events. Use `-SkipPurge` to disable that cleanup, or `-PurgeMapQueues` when you also want to clear the map verification queues before the run.

The same smoke check can be launched manually from GitHub Actions with the `aws-smoke` workflow. Configure the `CLOUDAMQP_PASSWORD` repository secret before running it.

**What it covers:**

| Step | What it tests |
|---|---|
| 1 | `POST /trucks` — truck registered, status AVAILABLE |
| 2 | `GET /trucks` — truck visible in read model |
| 3 | `truck.registered.v1` published to `trucks.exchange` |
| 4 | `warehouse.registered.v1` consumed — LocationResolver populated |
| 5 | `shipment.requested.v1` → truck changes to IN_TRANSIT |
| 6 | `truck.status.changed.v1 (DISPATCHED)` published to broker |
| 7 | `time.advanced.v1` ticks consumed — truck moves step by step |
| 8 | `truck.position.updated.v1` published to `trucks.exchange` |
| 9 | `delivery.completed.v1` + `truck.status.changed.v1 (RETURNED_TO_BASE)` |

> **Note:** The cleanup step targets `trucks.warehouse.registered`, `trucks.shipment.requested`, and `trucks.time.advanced` so previous E2E runs do not interfere with `LocationResolver` or truck movement.

---

## RabbitMQ Exchange Map

### Exchanges used by this service

| Exchange | Type | Purpose |
|---|---|---|
| `trucks.exchange` | Topic | Truck events published by transport-service |
| `shipments.exchange` | Topic | Shipment requests consumed and delivery completion events published by transport-service |

### External exchanges (declared by other services)

| Exchange | Owner | Events consumed |
|---|---|---|
| `ms-time.exchange` | Simulation (Rubén) | `time.advanced.v1` |
| `shipments.exchange` | Warehouses / Factories | `shipment.requested.v1`, `delivery.completed.v1` |
| `warehouses.exchange` | Warehouses | `warehouse.registered.v1` |

### Queues declared by this service

| Queue | Bound to | Routing key |
|---|---|---|
| `trucks.time.advanced` | `ms-time.exchange` | `time.advanced.v1` |
| `trucks.shipment.requested` | `shipments.exchange` | `shipment.requested.v1` |
| `trucks.warehouse.registered` | `warehouses.exchange` | `warehouse.registered.v1` |

---

## Legend

| Symbol | Meaning |
|---|---|
| `[+]` | Field added on top of the original design |
| `[-] MVP` | Removed in this version, to be recovered when BREAKDOWN is implemented |
| **Value Object** | No own identity (no ID). Identified solely by its attribute values. |

---

## PUBLISHED Contracts

Transport publishes truck events to `trucks.exchange` and delivery completion events to `shipments.exchange`.

---

### `truck.registered.v1`

Emitted when a new truck is registered via `POST /trucks`.

**Routing key:** `truck.registered.v1`
**Consumers:** Reporting | Map (ms-map)

| Field | Type | Notes |
|---|---|---|
| `truckId` | String (UUID) | Identifier of the new truck |
| `name` | String | Display name of the truck |
| `location` | Value Object | Starting position on the grid |
| `location.x` | Number | X coordinate |
| `location.y` | Number | Y coordinate |
| `capacity` | Integer | Maximum number of DeliveryItems the truck can carry |
| `timestamp` | Integer | Simulation day of registration (0 for pre-simulation registrations) |

```json
{
  "truckId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "name": "Truck 01",
  "location": { "x": 0, "y": 0 },
  "capacity": 10,
  "timestamp": 0
}
```

---

### `truck.status.changed.v1`

Notifies a change in a truck's operational status.

**Routing key:** `truck.status.changed.v1`
**Consumers:** Reporting

| Field | Type | Notes |
|---|---|---|
| `truckId` | String (UUID) | Truck whose status has changed |
| `oldStatus` | Enum | Previous status: `AVAILABLE` \| `IN_TRANSIT` (null on first registration) |
| `newStatus` | Enum | New status: `AVAILABLE` \| `IN_TRANSIT` |
| `position` | Value Object | Position at the moment of the change |
| `position.x` | Number | X coordinate |
| `position.y` | Number | Y coordinate |
| `currentLoad` | Integer | `[+]` Items currently loaded |
| `capacity` | Integer | `[+]` Maximum capacity |
| `timestamp` | Integer | Simulation day of the status change |
| `reason` | String | `TRUCK_REGISTERED` \| `DISPATCHED` \| `LOAD_UPDATED` \| `RETURNED_TO_BASE` |
| `reasonCode` | Enum | `[-] MVP` — Typed enum for BREAKDOWN phase |

**Status cycle:** `AVAILABLE` → `IN_TRANSIT` → `AVAILABLE`

> `DELIVERED` exists in the enum but is not used in the current MVP flow.

Example — truck registered:

```json
{
  "truckId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "oldStatus": null,
  "newStatus": "AVAILABLE",
  "position": { "x": 0, "y": 0 },
  "currentLoad": 0,
  "capacity": 10,
  "timestamp": 0,
  "reason": "TRUCK_REGISTERED"
}
```

Example — dispatched to a shipment:

```json
{
  "truckId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "oldStatus": "AVAILABLE",
  "newStatus": "IN_TRANSIT",
  "position": { "x": 0, "y": 0 },
  "currentLoad": 6,
  "capacity": 10,
  "timestamp": 3,
  "reason": "DISPATCHED"
}
```

Example — truck returns to base after all deliveries:

```json
{
  "truckId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "oldStatus": "IN_TRANSIT",
  "newStatus": "AVAILABLE",
  "position": { "x": 20, "y": 15 },
  "currentLoad": 0,
  "capacity": 10,
  "timestamp": 7,
  "reason": "RETURNED_TO_BASE"
}
```

---

### `truck.position.updated.v1`

Real-time truck position. High frequency — carries no business data.

**Routing key:** `truck.position.updated.v1`
**Consumers:** Map (ms-map) | Reporting

| Field | Type | Notes |
|---|---|---|
| `truckId` | String (UUID) | Identifies the truck to move on the map |
| `location` | Value Object | Current position |
| `location.x` | Number | X coordinate |
| `location.y` | Number | Y coordinate |

```json
{
  "truckId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "location": { "x": 5, "y": 3 }
}
```

---

### `delivery.completed.v1`

Confirms that a truck has completed a delivery at a destination warehouse.

**Exchange:** `shipments.exchange`
**Routing key:** `delivery.completed.v1`
**Consumers:** Warehouses | Reporting | Map (ms-map)

| Field | Type | Notes |
|---|---|---|
| `shipmentId` | String (UUID) | Correlates with the original `shipment.requested.v1` |
| `truckId` | String (UUID) | `[+]` Truck that completed the delivery |
| `items[]` | Array of Value Objects | Items delivered |
| `items[].productId` | String (UUID) | Product identifier |
| `items[].quantity` | Number | Quantity delivered |
| `location` | Value Object | Destination coordinates |
| `location.x` | Number | X coordinate |
| `location.y` | Number | Y coordinate |
| `completedAt` | Integer | `[+]` Simulation day of completion |

> Map (ms-map) only needs `truckId` to update the truck icon. Warehouses can ignore `location` — they know their own position.

```json
{
  "shipmentId": "f7e6d5c4-b3a2-1098-fedc-ba9876543210",
  "truckId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "items": [
    { "productId": "0f7b8f0f-5e80-4a9f-9d9d-2a5ad3a6d8b1", "quantity": 6 }
  ],
  "location": { "x": 8, "y": 2 },
  "completedAt": 5
}
```

---

## CONSUMED Contracts

---

### `time.advanced.v1`

Published by the Simulation service (Rubén) on each tick.

**Exchange:** `ms-time.exchange`
**Queue:** `trucks.time.advanced`
**Emitter:** Simulation (ms-time)

| Field | Type | Notes |
|---|---|---|
| `previousDay` | Integer | Day before this tick |
| `currentDay` | Integer | Current simulation day |
| `daysAdvanced` | Integer | Pre-calculated difference — used directly by the listener |

**Action:** For each `IN_TRANSIT` truck, advance `daysAdvanced` steps toward its delivery destination (Manhattan path: X first, then Y). Complete deliveries on arrival and return the truck to base.

```json
{
  "previousDay": 2,
  "currentDay": 3,
  "daysAdvanced": 1
}
```

---

### `shipment.requested.v1`

Request to transport materials between two locations.

**Exchange:** `shipments.exchange`
**Queue:** `trucks.shipment.requested`
**Emitter:** Warehouses / Factories

| Field | Type | Notes |
|---|---|---|
| `shipmentId` | String (UUID) | Correlates with `delivery.completed.v1` |
| `originId` | String | ID of the origin warehouse or factory |
| `destinationId` | String | ID of the destination warehouse |
| `items[]` | Array of Value Objects | Items to transport |
| `items[].productId` | String (UUID) | Product identifier |
| `items[].quantity` | Number | Quantity |
| `requestedAt` | Integer | Simulation day of the request |

**Action:** Resolve `originId` / `destinationId` to grid coordinates via `LocationResolver` → select optimal available truck → assign → publish `truck.status.changed.v1 (DISPATCHED)`.

```json
{
  "shipmentId": "c9d8e7f6-a5b4-3210-9876-543210fedcba",
  "originId": "warehouse-north-01",
  "destinationId": "warehouse-south-03",
  "items": [
    { "productId": "0f7b8f0f-5e80-4a9f-9d9d-2a5ad3a6d8b1", "quantity": 6 }
  ],
  "requestedAt": 3
}
```

---

### `warehouse.registered.v1`

Registers a warehouse location in the `LocationResolver` so it can be used when resolving shipment origins and destinations.

**Exchange:** `warehouses.exchange`
**Queue:** `trucks.warehouse.registered`
**Emitter:** Warehouses

| Field | Type | Notes |
|---|---|---|
| `warehouseId` | String | Unique warehouse identifier — matches `originId` / `destinationId` in shipments |
| `location` | Value Object | Warehouse position on the grid |
| `location.x` | Number | X coordinate |
| `location.y` | Number | Y coordinate |

```json
{
  "warehouseId": "warehouse-north-01",
  "location": { "x": 0, "y": 0 }
}
```

---

## REST Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/trucks` | Register a new truck. Publishes `truck.registered.v1` |
| `GET` | `/trucks` | List all trucks with current position and status |

Swagger UI: `http://localhost:8080/swagger-ui.html`

### `POST /trucks` — request body

```json
{
  "name": "Truck 01",
  "x": 0,
  "y": 0,
  "capacity": 10
}
```

### `GET /trucks` — response

```json
[
  {
    "truckId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "name": "Truck 01",
    "location": { "x": 3, "y": 5 },
    "status": "IN_TRANSIT"
  }
]
```

---

## Event Summary

| Event | Direction | Exchange | Consumers / Emitter |
|---|---|---|---|
| `truck.registered.v1` | PUBLISHES | `trucks.exchange` | Reporting \| Map (ms-map) |
| `truck.status.changed.v1` | PUBLISHES | `trucks.exchange` | Reporting |
| `truck.position.updated.v1` | PUBLISHES | `trucks.exchange` | Map (ms-map) \| Reporting |
| `delivery.completed.v1` | PUBLISHES | `shipments.exchange` | Warehouses \| Reporting \| Map (ms-map) |
| `time.advanced.v1` | CONSUMES | `ms-time.exchange` | Emitted by: Simulation (Rubén) |
| `shipment.requested.v1` | CONSUMES | `shipments.exchange` | Emitted by: Warehouses / Factories |
| `warehouse.registered.v1` | CONSUMES | `warehouses.exchange` | Emitted by: Warehouses |

---

## Note on Value Objects

`items[]` in `delivery.completed.v1` and `shipment.requested.v1` are Value Objects:

- No `itemId` — identified by their attribute values.
- Immutable — copied in full in each message.
- Structure: `{ productId: String, quantity: Number }`.
- `productId` is a UUID string shared across services.
